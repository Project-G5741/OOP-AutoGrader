package com.eiu.capstone.backend.grading;

import java.util.UUID;

record PendingFieldResult(UUID fieldId, boolean correct) {}

record PendingMethodResult(UUID methodId, boolean correct) {}

record PendingConstructorResult(UUID constructorId, boolean correct) {}

record PendingRelationResult(UUID relationId, boolean correct) {}

record PendingChallengeResult(UUID challengeId, boolean correct) {}
