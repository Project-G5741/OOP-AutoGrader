package com.eiu.capstone.backend.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.OopBlitzBankListResponse;
import com.eiu.capstone.backend.DTO.OopBlitzBankQuestionView;
import com.eiu.capstone.backend.DTO.OopBlitzBankStatsView;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionImportRequest;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionImportResponse;
import com.eiu.capstone.backend.DTO.OopBlitzQuestionUpsertRequest;
import com.eiu.capstone.backend.model.OopBlitzQuestion;
import com.eiu.capstone.backend.repository.OopBlitzQuestionRepository;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.BankStats;
import com.eiu.capstone.backend.service.OopBlitzQuestionBank.StoredQuestion;
import com.eiu.capstone.backend.utility.TimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lecturer and Admin CRUD + bulk JSON import for custom OOP Blitz questions.
 */
@Service
@Profile("!desktop & !desktop-lecturer")
public class LecturerOopBlitzQuestionService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final Set<String> VALID_KINDS = Set.of(
            OopBlitzQuestionBank.KIND_BUG,
            OopBlitzQuestionBank.KIND_OUTPUT,
            OopBlitzQuestionBank.KIND_UML,
            OopBlitzQuestionBank.KIND_MODIFIER);
    private static final Set<String> VALID_DIFFICULTIES = Set.of(
            OopBlitzQuestionBank.DIFF_EASY,
            OopBlitzQuestionBank.DIFF_MEDIUM,
            OopBlitzQuestionBank.DIFF_HARD);
    private static final int MAX_PROMPT_LEN = 400;
    private static final int MAX_EXPLANATION_LEN = 400;
    private static final int MAX_TOPIC_LEN = 64;
    private static final int MAX_CODE_LINES = 16;
    private static final int MAX_LINE_LEN = 240;
    private static final int MAX_OPTION_LEN = 180;
    private static final int MAX_IMPORT_BATCH = 500;

    private record ValidatedQuestion(
            String kind,
            String difficulty,
            String prompt,
            List<String> codeLines,
            List<String> options,
            int correctIndex,
            String explanation,
            String topic,
            boolean active) {
    }

    private final OopBlitzQuestionRepository repository;
    private final ObjectMapper objectMapper;

    public LecturerOopBlitzQuestionService(
            OopBlitzQuestionRepository repository,
            ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public OopBlitzBankListResponse listBank() {
        List<OopBlitzQuestion> rows = repository.findAllByOrderByUpdatedAtDesc();
        BankStats procStats = OopBlitzQuestionBank.proceduralStats();

        int customActive = 0;
        Map<String, Integer> byDiff = new LinkedHashMap<>();
        byDiff.put(OopBlitzQuestionBank.DIFF_EASY, 0);
        byDiff.put(OopBlitzQuestionBank.DIFF_MEDIUM, 0);
        byDiff.put(OopBlitzQuestionBank.DIFF_HARD, 0);

        List<OopBlitzBankQuestionView> views = new ArrayList<>(rows.size());
        for (OopBlitzQuestion row : rows) {
            if (row.isActive()) {
                customActive++;
            }
            byDiff.merge(row.getDifficulty(), 1, Integer::sum);
            views.add(toView(row));
        }

        OopBlitzBankStatsView stats = new OopBlitzBankStatsView(
                procStats.proceduralTemplateCount(),
                procStats.estimatedProceduralVariants(),
                rows.size(),
                customActive,
                procStats.proceduralByKind(),
                Map.copyOf(byDiff));
        return new OopBlitzBankListResponse(stats, List.copyOf(views));
    }

    @Transactional
    public OopBlitzBankQuestionView create(OopBlitzQuestionUpsertRequest request, UUID actorId) {
        ValidatedQuestion valid = validate(request);
        OffsetDateTime now = TimeUtil.nowInVietnam();
        OopBlitzQuestion entity = new OopBlitzQuestion();
        entity.setId(UUID.randomUUID());
        entity.setCreatedBy(actorId);
        entity.setCreatedAt(now);
        applyValidated(entity, valid, now);
        return toView(repository.save(entity));
    }

    @Transactional
    public OopBlitzBankQuestionView update(UUID id, OopBlitzQuestionUpsertRequest request) {
        OopBlitzQuestion entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        ValidatedQuestion valid = validate(request);
        applyValidated(entity, valid, TimeUtil.nowInVietnam());
        return toView(repository.save(entity));
    }

    @Transactional
    public OopBlitzBankQuestionView setActive(UUID id, boolean active) {
        OopBlitzQuestion entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
        entity.setActive(active);
        entity.setUpdatedAt(TimeUtil.nowInVietnam());
        return toView(repository.save(entity));
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found");
        }
        repository.deleteById(id);
    }

    @Transactional
    public OopBlitzQuestionImportResponse bulkImport(OopBlitzQuestionImportRequest request, UUID actorId) {
        List<OopBlitzQuestionUpsertRequest> items = request == null ? null : request.questions();
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import list cannot be empty.");
        }
        if (items.size() > MAX_IMPORT_BATCH) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Import batch exceeds limit of " + MAX_IMPORT_BATCH + " questions.");
        }

        OffsetDateTime now = TimeUtil.nowInVietnam();
        List<OopBlitzQuestion> toSave = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            try {
                ValidatedQuestion valid = validate(items.get(i));
                OopBlitzQuestion entity = new OopBlitzQuestion();
                entity.setId(UUID.randomUUID());
                entity.setCreatedBy(actorId);
                entity.setCreatedAt(now);
                applyValidated(entity, valid, now);
                toSave.add(entity);
            } catch (ResponseStatusException ex) {
                errors.add("Item #" + (i + 1) + ": " + ex.getReason());
            }
        }

        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }
        return new OopBlitzQuestionImportResponse(toSave.size(), errors.size(), List.copyOf(errors));
    }

    @Transactional(readOnly = true)
    public List<StoredQuestion> loadActiveCustomQuestions() {
        List<OopBlitzQuestion> activeRows = repository.findByActiveTrueOrderByUpdatedAtDesc();
        List<StoredQuestion> out = new ArrayList<>(activeRows.size());
        for (OopBlitzQuestion row : activeRows) {
            out.add(toStoredQuestion(row, objectMapper));
        }
        return List.copyOf(out);
    }

    static StoredQuestion toStoredQuestion(OopBlitzQuestion row, ObjectMapper mapper) {
        List<String> code = readStringList(row.getCodeLines(), mapper);
        List<String> options = readStringList(row.getOptionsJson(), mapper);
        int timeLimitSec = OopBlitzQuestionBank.DIFF_HARD.equals(row.getDifficulty()) ? 20 : 15;
        return new StoredQuestion(
                "custom:" + row.getId(),
                row.getKind(),
                row.getDifficulty(),
                timeLimitSec,
                row.getPrompt(),
                code,
                options,
                row.getCorrectIndex(),
                row.getExplanation());
    }

    private void applyValidated(OopBlitzQuestion entity, ValidatedQuestion valid, OffsetDateTime now) {
        entity.setKind(valid.kind());
        entity.setDifficulty(valid.difficulty());
        entity.setPrompt(valid.prompt());
        entity.setCodeLines(writeJson(valid.codeLines()));
        entity.setOptionsJson(writeJson(valid.options()));
        entity.setCorrectIndex(valid.correctIndex());
        entity.setExplanation(valid.explanation());
        entity.setTopic(valid.topic());
        entity.setActive(valid.active());
        entity.setUpdatedAt(now);
    }

    private OopBlitzBankQuestionView toView(OopBlitzQuestion row) {
        return new OopBlitzBankQuestionView(
                row.getId(),
                row.getKind(),
                row.getDifficulty(),
                row.getPrompt(),
                readStringList(row.getCodeLines(), objectMapper),
                readStringList(row.getOptionsJson(), objectMapper),
                row.getCorrectIndex(),
                row.getExplanation(),
                row.getTopic(),
                row.isActive(),
                TimeUtil.formatDisplayTimestamp(row.getCreatedAt()),
                TimeUtil.formatDisplayTimestamp(row.getUpdatedAt()));
    }

    private static ValidatedQuestion validate(OopBlitzQuestionUpsertRequest req) {
        if (req == null) {
            throw badRequest("Question payload is required.");
        }
        String kind = normalizeEnum(req.kind(), VALID_KINDS, "Invalid question kind (BUG, OUTPUT, UML, MODIFIER).");
        String difficulty = normalizeEnum(req.difficulty(), VALID_DIFFICULTIES, "Invalid difficulty (EASY, MEDIUM, HARD).");
        String prompt = requireBoundedText(req.prompt(), MAX_PROMPT_LEN, "Prompt is required.");
        String explanation = requireBoundedText(req.explanation(), MAX_EXPLANATION_LEN, "Explanation is required.");

        if (req.codeLines() == null || req.codeLines().isEmpty() || req.codeLines().size() > MAX_CODE_LINES) {
            throw badRequest("Provide between 1 and " + MAX_CODE_LINES + " code lines.");
        }
        List<String> codeLines = new ArrayList<>(req.codeLines().size());
        boolean hasNonBlankCode = false;
        for (String rawLine : req.codeLines()) {
            String line = rawLine == null ? "" : rawLine.replace("\r", "");
            if (line.length() > MAX_LINE_LEN) {
                throw badRequest("Code line exceeds " + MAX_LINE_LEN + " characters.");
            }
            if (!line.isBlank()) {
                hasNonBlankCode = true;
            }
            codeLines.add(line);
        }
        if (!hasNonBlankCode) {
            throw badRequest("Code snippet cannot be completely blank.");
        }

        Integer rawCorrect = req.correctIndex();
        if (rawCorrect == null) {
            throw badRequest("Correct answer index is required.");
        }
        int correctIndex = rawCorrect;

        List<String> options;
        if (OopBlitzQuestionBank.KIND_BUG.equals(kind)) {
            if (correctIndex < 0 || correctIndex >= codeLines.size()) {
                throw badRequest("Bug line index must be between 1 and " + codeLines.size() + ".");
            }
            options = List.of();
        } else {
            if (req.options() == null || req.options().size() != 4) {
                throw badRequest("Choice questions must provide exactly 4 options.");
            }
            options = new ArrayList<>(4);
            Set<String> unique = new HashSet<>();
            for (String rawOpt : req.options()) {
                String trimmed = rawOpt == null ? "" : rawOpt.trim();
                if (trimmed.isEmpty() || trimmed.length() > MAX_OPTION_LEN) {
                    throw badRequest("Each option must be 1 to " + MAX_OPTION_LEN + " characters.");
                }
                if (!unique.add(trimmed)) {
                    throw badRequest("All 4 options must be distinct.");
                }
                options.add(trimmed);
            }
            if (correctIndex < 0 || correctIndex > 3) {
                throw badRequest("Correct option index must be between 0 and 3.");
            }
        }

        String topic = null;
        if (req.topic() != null && !req.topic().isBlank()) {
            topic = req.topic().trim();
            if (topic.length() > MAX_TOPIC_LEN) {
                throw badRequest("Topic cannot exceed " + MAX_TOPIC_LEN + " characters.");
            }
        }
        boolean active = req.active() == null || req.active();
        return new ValidatedQuestion(
                kind,
                difficulty,
                prompt,
                List.copyOf(codeLines),
                List.copyOf(options),
                correctIndex,
                explanation,
                topic,
                active);
    }

    private static String normalizeEnum(String raw, Set<String> allowed, String error) {
        if (raw == null || raw.isBlank()) {
            throw badRequest(error);
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(upper)) {
            throw badRequest(error);
        }
        return upper;
    }

    private static String requireBoundedText(String raw, int maxLen, String emptyError) {
        if (raw == null || raw.trim().isEmpty()) {
            throw badRequest(emptyError);
        }
        String trimmed = raw.trim();
        if (trimmed.length() > maxLen) {
            throw badRequest("Text exceeds " + maxLen + " characters.");
        }
        return trimmed;
    }

    private static ResponseStatusException badRequest(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private String writeJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OOP Blitz question field", ex);
        }
    }

    private static List<String> readStringList(String raw, ObjectMapper mapper) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(raw, STRING_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Corrupt OOP Blitz question payload", ex);
        }
    }
}
