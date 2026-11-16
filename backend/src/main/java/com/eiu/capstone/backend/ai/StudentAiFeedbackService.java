package com.eiu.capstone.backend.ai;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.ClassDetailDTO;
import com.eiu.capstone.backend.DTO.ClassTabResponse;
import com.eiu.capstone.backend.service.DisclosureMode;
import com.eiu.capstone.backend.model.Challenge;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.LabSubmission;
import com.eiu.capstone.backend.repository.AiFeedbackCacheRepository;
import com.eiu.capstone.backend.repository.AiStudentCredentialRepository;
import com.eiu.capstone.backend.repository.ChallengeRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.LabSubmissionRepository;
import com.eiu.capstone.backend.service.ClassStructureService;
import com.eiu.capstone.backend.service.SubmissionResolutionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Profile("!desktop")
public class StudentAiFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(StudentAiFeedbackService.class);

    private final boolean globallyEnabled;
    private final String policyVersion;
    private final int cacheRetentionDays;
    private final long defaultTokenBudget;
    private final ObjectMapper objectMapper;
    private final LabRepository labRepository;
    private final ChallengeRepository challengeRepository;
    private final LabSubmissionRepository labSubmissionRepository;
    private final SubmissionResolutionService submissionResolutionService;
    private final ClassStructureService classStructureService;
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
            @Value("${app.ai.policy-version:challenge-v2}") String policyVersion,
            @Value("${app.ai.cache-retention-days:30}") int cacheRetentionDays,
            @Value("${app.ai.token-budget:1000000}") long defaultTokenBudget,
            ObjectMapper objectMapper,
            LabRepository labRepository,
            ChallengeRepository challengeRepository,
            LabSubmissionRepository labSubmissionRepository,
            SubmissionResolutionService submissionResolutionService,
            ClassStructureService classStructureService,
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
        this.challengeRepository = challengeRepository;
        this.labSubmissionRepository = labSubmissionRepository;
        this.submissionResolutionService = submissionResolutionService;
        this.classStructureService = classStructureService;
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
        geminiClient.validateApiKey(trimmed);

        Instant now = Instant.now();
        AiStudentCredential row = credentialRepository.findByStudentId(studentId).orElseGet(AiStudentCredential::new);
        if (row.getId() == null) {
            row.setId(UUID.randomUUID());
            row.setStudentId(studentId);
            row.setCreatedAt(now);
            row.setTokenBudget(defaultTokenBudget);
        }
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
            UUID challengeId,
            String challengeName,
            List<FailingClassInfo> clientFailingClasses) {
        requireGlobal();
        Lab lab = labRepository.findById(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        if (!lab.isAiEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AI assistance is disabled for this lab");
        }
        UUID resolved = submissionResolutionService.resolveSubmissionId(labId, studentId, submissionId);
        if (resolved == null && submissionId != null) {
            // Client sent a stale/purged submissionId — fall back to the student's latest attempt.
            log.debug("AI hint: submissionId {} not found for student {}; falling back to latest", submissionId, studentId);
            resolved = submissionResolutionService.resolveLatestSubmissionId(labId, studentId);
        }
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found");
        }
        LabSubmission submission = labSubmissionRepository.findById(resolved)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Submission not found"));

        Challenge challenge = null;
        if (challengeId != null) {
            challenge = challengeRepository.findById(challengeId).orElse(null);
        }
        if (challenge == null) {
            List<Challenge> challenges = challengeRepository.findByLab_IdOrderByChallengeNumberAsc(labId);
            if (!challenges.isEmpty()) {
                challenge = challenges.get(0);
            }
        }

        String resolvedChallengeName = challengeName != null && !challengeName.isBlank()
                ? challengeName.trim()
                : (challenge != null ? challenge.getName() : "Challenge");
        String cacheKey = challenge != null ? challenge.getId().toString() : (challengeId != null ? challengeId.toString() : "CHALLENGE");

        // Resolve failing classes: prefer client-sent if available, else derive from ClassStructureService
        List<FailingClassInfo> failingClasses = clientFailingClasses != null ? new ArrayList<>(clientFailingClasses) : new ArrayList<>();
        if (failingClasses.isEmpty() && challenge != null) {
            try {
                ClassTabResponse tab = classStructureService.getClassData(labId, challenge.getId(), studentId, resolved, DisclosureMode.STUDENT);
                if (tab != null && tab.classes() != null) {
                    failingClasses = extractFailingClasses(tab.classes());
                }
            } catch (Exception ex) {
                log.warn("Could not load class data for AI feedback: {}", ex.getMessage());
            }
        }

        // If no classes have errors, return a success summary immediately with no token burn
        if (failingClasses.isEmpty()) {
            ChallengeAiFeedbackPayload perfectPayload = new ChallengeAiFeedbackPayload(
                    "All classes in " + resolvedChallengeName + " passed declaration checks! Excellent work.",
                    List.of(),
                    "en");
            return new HintResponse(perfectPayload, true, "completed", null, tokenStatusFor(studentId), List.of());
        }

        // Cache lookup
        var cached = cacheRepository.findByStudentIdAndSubmissionIdAndLearningAreaAndLanguageAndPolicyVersion(
                studentId, resolved, cacheKey, "en", policyVersion);
        if (cached.isPresent() && cached.get().getExpiresAt().isAfter(Instant.now())) {
            ChallengeAiFeedbackPayload payload = readChallengePayload(cached.get().getPayloadJson());
            if (payload != null) {
                return new HintResponse(payload, true, "completed", null, tokenStatusFor(studentId), List.of());
            }
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

        String apiKey;
        try {
            apiKey = credentialCrypto.decrypt(credential.getEncryptedApiKey());
        } catch (ResponseStatusException ex) {
            // Stored ciphertext is unreadable (server secret rotation, data corruption, etc.).
            // Clear the bad credential so the student can re-import without manual DB cleanup.
            log.warn("Decrypt failed for student {}; clearing corrupt credential. cause={}", studentId, ex.getReason());
            credentialRepository.deleteByStudentId(studentId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Your stored API key could not be read (the server encryption key may have changed). Please re-import your Gemini API key.");
        }
        Map<String, Object> context = contextBuilder.buildChallengeContext(
                lab.getName(), resolvedChallengeName, failingClasses);

        GeminiClient.GeminiResult gemini;
        try {
            gemini = geminiClient.generateJson(apiKey, promptPolicy.systemInstruction(policyVersion), context);
        } catch (ResponseStatusException ex) {
            ChallengeAiFeedbackPayload fallback = AiFallbackMessages.forChallenge(resolvedChallengeName, failingClasses);
            return new HintResponse(fallback, false, "failed", ex.getReason(), toTokenStatus(credential), List.of());
        }

        List<String> alerts = AiTokenBudget.applyUsageAndCollectAlerts(credential, gemini.totalTokenCount());
        credential.setUpdatedAt(Instant.now());
        credentialRepository.save(credential);

        ChallengeAiFeedbackPayload parsed = parseChallengeModelJson(gemini.text(), resolvedChallengeName, failingClasses);
        Set<String> prohibited = prohibitedIdentifierLoader.loadForLab(labId);
        Set<String> allowedClassNames = failingClasses.stream()
                .map(FailingClassInfo::className)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        var validation = responseValidator.validateChallengePayload(parsed, prohibited, allowedClassNames);
        if (!validation.passed()) {
            ChallengeAiFeedbackPayload fallback = AiFallbackMessages.forChallenge(resolvedChallengeName, failingClasses);
            return new HintResponse(fallback, false, "rejected", validation.reason(), toTokenStatus(credential), alerts);
        }

        Instant now = Instant.now();
        AiFeedbackCache row = cached.orElseGet(AiFeedbackCache::new);
        if (row.getId() == null) {
            row.setId(UUID.randomUUID());
            row.setStudentId(studentId);
            row.setSubmissionId(resolved);
            row.setLearningArea(cacheKey);
            row.setLanguage("en");
            row.setPolicyVersion(policyVersion);
            row.setCreatedAt(now);
        }
        row.setModelIdentifier(geminiClient.modelId());
        row.setPayloadJson(writeChallengePayload(parsed));
        row.setExpiresAt(now.plus(cacheRetentionDays, ChronoUnit.DAYS));
        cacheRepository.save(row);

        if (!submission.getId().equals(resolved)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Submission mismatch");
        }
        return new HintResponse(parsed, false, "completed", null, toTokenStatus(credential), alerts);
    }

    private List<FailingClassInfo> extractFailingClasses(List<ClassDetailDTO> classes) {
        if (classes == null) return List.of();
        List<FailingClassInfo> result = new ArrayList<>();
        for (ClassDetailDTO cls : classes) {
            boolean hasCompileErr = cls.error() != null && !cls.error().isBlank();
            boolean hasFieldErr = cls.fields() != null && cls.fields().stream().anyMatch(f -> !f.ok());
            boolean hasCtorErr = cls.constructors() != null && cls.constructors().stream().anyMatch(c -> !c.ok());
            boolean hasMethodErr = cls.methods() != null && cls.methods().stream().anyMatch(m -> !m.ok());
            boolean isErrStatus = "error".equalsIgnoreCase(cls.status()) || "warning".equalsIgnoreCase(cls.status());

            if (hasCompileErr || hasFieldErr || hasCtorErr || hasMethodErr || isErrStatus) {
                List<String> fieldIssues = cls.fields() == null ? List.of() :
                        cls.fields().stream().filter(f -> !f.ok()).map(f -> f.name() + " (" + f.dataType() + ")").toList();
                List<String> ctorIssues = cls.constructors() == null ? List.of() :
                        cls.constructors().stream().filter(c -> !c.ok()).map(c -> c.name() + "(" + (c.params() != null ? c.params() : "") + ")").toList();
                List<String> methodIssues = cls.methods() == null ? List.of() :
                        cls.methods().stream().filter(m -> !m.ok()).map(m -> m.name() + " -> " + m.returnType()).toList();

                result.add(new FailingClassInfo(
                        cls.name(),
                        cls.type(),
                        cls.status(),
                        cls.error(),
                        fieldIssues,
                        ctorIssues,
                        methodIssues
                ));
            }
        }
        return result;
    }

    private ChallengeAiFeedbackPayload parseChallengeModelJson(
            String jsonText,
            String challengeName,
            List<FailingClassInfo> failingClasses) {
        try {
            JsonNode root = objectMapper.readTree(jsonText);
            String summary = root.path("challengeSummary").asText(
                    root.path("summary").asText("Guidance for " + (challengeName != null ? challengeName : "this challenge")));
            List<ClassGuidanceItem> classItems = new ArrayList<>();
            JsonNode classesNode = root.path("classes");
            if (classesNode.isArray()) {
                for (JsonNode cNode : classesNode) {
                    String className = cNode.path("className").asText("");
                    String classType = cNode.path("classType").asText("CLASS");
                    String compileError = cNode.hasNonNull("compileError") ? cNode.path("compileError").asText() : null;
                    String status = cNode.path("status").asText("error");

                    if (failingClasses != null) {
                        for (FailingClassInfo f : failingClasses) {
                            if (f.className() != null && f.className().equalsIgnoreCase(className)) {
                                if (compileError == null) compileError = f.compileError();
                                if (f.classType() != null && !f.classType().isBlank()) classType = f.classType();
                                if (f.status() != null && !f.status().isBlank()) status = f.status();
                                break;
                            }
                        }
                    }

                    List<GuidanceDetail> guidance = new ArrayList<>();
                    JsonNode gNode = cNode.path("guidance");
                    if (gNode.isArray()) {
                        for (JsonNode item : gNode) {
                            String title = item.path("title").asText("Guidance");
                            String explanation = item.path("explanation").asText("");
                            String question = item.path("guidingQuestion").asText("");
                            if (!explanation.isBlank()) {
                                guidance.add(new GuidanceDetail(title, explanation, question));
                            }
                        }
                    }
                    if (!className.isBlank()) {
                        classItems.add(new ClassGuidanceItem(className, classType, status, compileError, guidance));
                    }
                }
            }

            // Ensure all failing classes are represented
            if (failingClasses != null) {
                for (FailingClassInfo fc : failingClasses) {
                    boolean exists = classItems.stream().anyMatch(ci -> ci.className().equalsIgnoreCase(fc.className()));
                    if (!exists) {
                        ChallengeAiFeedbackPayload singleFallback = AiFallbackMessages.forChallenge(challengeName, List.of(fc));
                        if (!singleFallback.classes().isEmpty()) {
                            classItems.add(singleFallback.classes().get(0));
                        }
                    }
                }
            }

            return new ChallengeAiFeedbackPayload(summary, classItems, "en");
        } catch (Exception ex) {
            log.warn("Failed to parse Gemini challenge JSON: {}", ex.getMessage());
            return AiFallbackMessages.forChallenge(challengeName, failingClasses);
        }
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
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI assistant is disabled");
        }
    }

    private String writeChallengePayload(ChallengeAiFeedbackPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize AI payload");
        }
    }

    private ChallengeAiFeedbackPayload readChallengePayload(String json) {
        try {
            return objectMapper.readValue(json, ChallengeAiFeedbackPayload.class);
        } catch (Exception ex) {
            return null;
        }
    }

    public record FeatureStatus(
            boolean globallyEnabled,
            boolean labEnabled,
            String policyVersion,
            String modelIdentifier,
            TokenStatus token) {}

    public record TokenStatus(
            boolean connected,
            String keyHint,
            long tokensUsed,
            long tokenBudget,
            int usagePercent,
            boolean exhausted,
            List<String> alerts) {

        public static TokenStatus disconnected(long budget) {
            return new TokenStatus(false, null, 0L, budget, 0, false, List.of());
        }
    }

    public record HintResponse(
            ChallengeAiFeedbackPayload feedback,
            boolean cacheHit,
            String status,
            String reason,
            TokenStatus token,
            List<String> alerts) {}
}
