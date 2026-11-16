package com.eiu.capstone.backend.ai;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.repository.AiFeedbackCacheRepository;
import com.eiu.capstone.backend.repository.AiStudentCredentialRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.service.SubmissionResolutionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Profile("!desktop")
public class StudentAiFeedbackService {

    private final boolean globallyEnabled;
    private final String policyVersion;
    private final int cacheRetentionDays;
    private final long defaultTokenBudget;
    private final ObjectMapper objectMapper;
    private final LabRepository labRepository;
    private final LabSubmissionRepository labSubmissionRepository;
    private final SubmissionResolutionService submissionResolutionService;
    private final AiFeedbackCacheRepository cacheRepository;
    private final AiStudentCredentialRepository credentialRepository;
    private final AiCredentialCrypto credentialCrypto;
    private final AiContextBuilder contextBuilder;
    private final AiPromptPolicy promptPolicy;
    private final GeminiClient geminiClient;
    private final AiResponseValidator responseValidator;
    private final AiProhibitedIdentifierLoader prohibitedIdentifierLoader;

    public StudentAiFeedbackService(
            @Value("${app.ai.enabled:true}") boolean globallyEnabled,
            @Value("${app.ai.policy-version:conceptual-v1}") String policyVersion,
            @Value("${app.ai.cache-retention-days:30}") int cacheRetentionDays,
            @Value("${app.ai.token-budget:1000000}") long defaultTokenBudget,
            ObjectMapper objectMapper,
            LabRepository labRepository,
            LabSubmissionRepository labSubmissionRepository,
            SubmissionResolutionService submissionResolutionService,
            AiFeedbackCacheRepository cacheRepository,
            AiStudentCredentialRepository credentialRepository,
            AiCredentialCrypto credentialCrypto,
            AiContextBuilder contextBuilder,
            AiPromptPolicy promptPolicy,
            GeminiClient geminiClient,
            AiResponseValidator responseValidator,
            AiProhibitedIdentifierLoader prohibitedIdentifierLoader) {
        this.globallyEnabled = globallyEnabled;
        this.policyVersion = policyVersion;
        this.cacheRetentionDays = cacheRetentionDays;
        this.defaultTokenBudget = Math.max(1L, defaultTokenBudget);
        this.objectMapper = objectMapper;
        this.labRepository = labRepository;
        this.labSubmissionRepository = labSubmissionRepository;
        this.submissionResolutionService = submissionResolutionService;
        this.cacheRepository = cacheRepository;
        this.credentialRepository = credentialRepository;
        this.credentialCrypto = credentialCrypto;
        this.contextBuilder = contextBuilder;
        this.promptPolicy = promptPolicy;
        this.geminiClient = geminiClient;
        this.responseValidator = responseValidator;
        this.prohibitedIdentifierLoader = prohibitedIdentifierLoader;
    }

    public FeatureStatus status(UUID studentId, UUID labId) {
        boolean labEnabled = false;
        if (labId != null) {
            labEnabled = labRepository.findById(labId).map(Lab::isAiEnabled).orElse(false);
        }
        TokenStatus token = tokenStatusFor(studentId);
        return new FeatureStatus(globallyEnabled, labEnabled, policyVersion, geminiClient.modelId(), token);
    }

    public TokenStatus tokenStatusFor(UUID studentId) {
        return credentialRepository.findByStudentId(studentId)
                .map(this::toTokenStatus)
                .orElseGet(() -> TokenStatus.disconnected(defaultTokenBudget));
    }

    public TokenStatus saveKey(UUID studentId, String apiKey) {
        requireGlobal();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini API key is required");
        }
        String trimmed = apiKey.trim();
        // Fast GET /models/{id} — do not burn generateContent (or thinking latency) on Import.
        geminiClient.validateApiKey(trimmed);

        Instant now = Instant.now();
        AiStudentCredential row = credentialRepository.findByStudentId(studentId).orElseGet(AiStudentCredential::new);
        if (row.getId() == null) {
            row.setId(UUID.randomUUID());
            row.setStudentId(studentId);
            row.setCreatedAt(now);
            row.setTokenBudget(defaultTokenBudget);
        }
        // Import / replace starts a fresh budget window (matches exhausted “import a new key” path).
        row.setTokensUsed(0L);
        row.setNotified50(false);
        row.setNotified80(false);
        row.setNotified90(false);
        row.setEncryptedApiKey(credentialCrypto.encrypt(trimmed));
        row.setKeyHint(AiCredentialCrypto.hintFor(trimmed));
        row.setUpdatedAt(now);
        credentialRepository.save(row);
        return toTokenStatus(row);
    }

    @Transactional
    public TokenStatus disconnect(UUID studentId) {
        credentialRepository.deleteByStudentId(studentId);
        return TokenStatus.disconnected(defaultTokenBudget);
    }

    @Transactional
    public HintResponse requestHint(
            UUID studentId,
            UUID labId,
            UUID submissionId,
            String learningAreaRaw,
            String languageRaw) {
        requireGlobal();
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        if (!lab.isAiEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AI assistance is disabled for this lab");
        }
        UUID resolved = submissionResolutionService.resolveSubmissionId(labId, studentId, submissionId);
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found");
        }
        LabSubmission submission = labSubmissionRepository.findById(resolved)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        AiLearningArea area;
        try {
            area = AiLearningArea.require(learningAreaRaw);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
        String language = normalizeLanguage(languageRaw);

        var cached = cacheRepository.findByStudentIdAndSubmissionIdAndLearningAreaAndLanguageAndPolicyVersion(
                studentId, resolved, area.name(), language, policyVersion);
        if (cached.isPresent() && cached.get().getExpiresAt().isAfter(Instant.now())) {
            ConceptualHintPayload payload = readPayload(cached.get().getPayloadJson());
            return new HintResponse(payload, true, "completed", null, tokenStatusFor(studentId), List.of());
        }

        AiStudentCredential credential = credentialRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Import a Gemini API key before requesting hints"));
        if (AiTokenBudget.isExhausted(credential.getTokensUsed(), credential.getTokenBudget())) {
            if (!credential.isNotified90()) {
                credential.setNotified90(true);
                credential.setUpdatedAt(Instant.now());
                credentialRepository.save(credential);
            }
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Token budget exhausted (90%). Import a new key or raise the budget.");
        }

        String apiKey = credentialCrypto.decrypt(credential.getEncryptedApiKey());
        Map<String, Object> context = contextBuilder.build(lab.getName(), area, language, policyVersion);
        GeminiClient.GeminiResult gemini;
        try {
            gemini = geminiClient.generateJson(apiKey, promptPolicy.systemInstruction(policyVersion), context);
        } catch (ResponseStatusException ex) {
            ConceptualHintPayload fallback = AiFallbackMessages.forArea(area, language);
            return new HintResponse(fallback, false, "failed", ex.getReason(), toTokenStatus(credential), List.of());
        }

        List<String> alerts = AiTokenBudget.applyUsageAndCollectAlerts(credential, gemini.totalTokenCount());
        credential.setUpdatedAt(Instant.now());
        credentialRepository.save(credential);

        ConceptualHintParsed parsed = parseModelJson(gemini.text());
        Set<String> prohibited = prohibitedIdentifierLoader.loadForLab(labId);
        var validation = responseValidator.validate(
                parsed.summary(), parsed.question(), parsed.explanation(), prohibited);
        if (!validation.passed()) {
            ConceptualHintPayload fallback = AiFallbackMessages.forArea(area, language);
            return new HintResponse(
                    fallback, false, "rejected", validation.reason(), toTokenStatus(credential), alerts);
        }

        ConceptualHintPayload payload = new ConceptualHintPayload(
                parsed.summary().trim(),
                area.name(),
                parsed.concept() == null || parsed.concept().isBlank() ? defaultConcept(area) : parsed.concept().trim(),
                parsed.question().trim(),
                parsed.explanation() == null ? "" : parsed.explanation().trim(),
                language);

        Instant now = Instant.now();
        AiFeedbackCache row = cached.orElseGet(AiFeedbackCache::new);
        if (row.getId() == null) {
            row.setId(UUID.randomUUID());
            row.setStudentId(studentId);
            row.setSubmissionId(resolved);
            row.setLearningArea(area.name());
            row.setLanguage(language);
            row.setPolicyVersion(policyVersion);
            row.setCreatedAt(now);
        }
        row.setModelIdentifier(geminiClient.modelId());
        row.setPayloadJson(writePayload(payload));
        row.setExpiresAt(now.plus(cacheRetentionDays, ChronoUnit.DAYS));
        cacheRepository.save(row);

        if (!submission.getId().equals(resolved)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Submission mismatch");
        }
        return new HintResponse(payload, false, "completed", null, toTokenStatus(credential), alerts);
    }

    private TokenStatus toTokenStatus(AiStudentCredential credential) {
        long used = credential.getTokensUsed();
        long budget = credential.getTokenBudget();
        return new TokenStatus(
                true,
                credential.getKeyHint(),
                used,
                budget,
                AiTokenBudget.usagePercent(used, budget),
                AiTokenBudget.isExhausted(used, budget),
                List.of());
    }

    private void requireGlobal() {
        if (!globallyEnabled) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AI assistance is disabled");
        }
    }

    private static String normalizeLanguage(String languageRaw) {
        if (languageRaw == null || languageRaw.isBlank()) {
            return "en";
        }
        String lower = languageRaw.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("vi")) {
            return "vi";
        }
        return "en";
    }

    private static String defaultConcept(AiLearningArea area) {
        return switch (area) {
            case FIELD_MODELING -> "Data modelling";
            case METHODS -> "Object responsibilities";
            case CONSTRUCTORS -> "Object initialisation";
        };
    }

    private ConceptualHintParsed parseModelJson(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            return new ConceptualHintParsed(
                    text(node, "summary"),
                    text(node, "concept"),
                    text(node, "question"),
                    text(node, "explanation"));
        } catch (Exception ex) {
            return new ConceptualHintParsed("", "", "", "");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private String writePayload(ConceptualHintPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store AI feedback");
        }
    }

    private ConceptualHintPayload readPayload(String json) {
        try {
            return objectMapper.readValue(json, ConceptualHintPayload.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Corrupt AI cache");
        }
    }

    public record FeatureStatus(
            boolean globallyEnabled,
            boolean labEnabled,
            String policyVersion,
            String model,
            TokenStatus token) {}

    public record TokenStatus(
            boolean connected,
            String keyHint,
            long tokensUsed,
            long tokenBudget,
            int usagePercent,
            boolean exhausted,
            List<String> alerts) {

        static TokenStatus disconnected(long budget) {
            return new TokenStatus(false, null, 0L, budget, 0, false, List.of());
        }

        TokenStatus withAlerts(List<String> nextAlerts) {
            return new TokenStatus(
                    connected, keyHint, tokensUsed, tokenBudget, usagePercent, exhausted,
                    nextAlerts == null ? List.of() : List.copyOf(nextAlerts));
        }
    }

    public record HintResponse(
            ConceptualHintPayload feedback,
            boolean cacheHit,
            String status,
            String errorCategory,
            TokenStatus token,
            List<String> alerts) {}

    private record ConceptualHintParsed(String summary, String concept, String question, String explanation) {}
}
