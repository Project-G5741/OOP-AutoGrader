package com.eiu.capstone.backend.grading;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MmdGradingOutcome {
    private final Map<UUID, Boolean> classCorrect = new HashMap<>();
    private final Map<UUID, Boolean> classPresent = new HashMap<>();
    private final Map<UUID, Boolean> fieldCorrect = new HashMap<>();
    private final Map<UUID, Boolean> methodCorrect = new HashMap<>();
    private final Map<UUID, Boolean> constructorCorrect = new HashMap<>();
    private final Map<UUID, Boolean> relationCorrect = new HashMap<>();

    public void setClass(UUID classId, boolean correct) { classCorrect.put(classId, correct); }
    public void setClassPresent(UUID classId, boolean present) { classPresent.put(classId, present); }
    public void setField(UUID fieldId, boolean correct) { fieldCorrect.put(fieldId, correct); }
    public void setMethod(UUID methodId, boolean correct) { methodCorrect.put(methodId, correct); }
    public void setConstructor(UUID constructorId, boolean correct) { constructorCorrect.put(constructorId, correct); }
    public void setRelation(UUID relationId, boolean correct) { relationCorrect.put(relationId, correct); }

    public boolean isClassCorrect(UUID classId) { return classCorrect.getOrDefault(classId, false); }
    public boolean isClassPresent(UUID classId) { return classPresent.getOrDefault(classId, false); }
    public boolean isFieldCorrect(UUID fieldId) { return fieldCorrect.getOrDefault(fieldId, false); }
    public boolean isMethodCorrect(UUID methodId) { return methodCorrect.getOrDefault(methodId, false); }
    public boolean isConstructorCorrect(UUID constructorId) { return constructorCorrect.getOrDefault(constructorId, false); }
    public boolean isRelationCorrect(UUID relationId) { return relationCorrect.getOrDefault(relationId, false); }

    public static MmdGradingOutcome allIncorrect(ChallengeRubricElements elements) {
        MmdGradingOutcome outcome = new MmdGradingOutcome();
        elements.classIds().forEach(id -> {
            outcome.setClass(id, false);
            outcome.setClassPresent(id, false);
        });
        elements.fieldIds().forEach(id -> outcome.setField(id, false));
        elements.methodIds().forEach(id -> outcome.setMethod(id, false));
        elements.constructorIds().forEach(id -> outcome.setConstructor(id, false));
        elements.relationIds().forEach(id -> outcome.setRelation(id, false));
        return outcome;
    }

    public record ChallengeRubricElements(
            List<UUID> classIds,
            List<UUID> fieldIds,
            List<UUID> methodIds,
            List<UUID> constructorIds,
            List<UUID> relationIds) {}
}
