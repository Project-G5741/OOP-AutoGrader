package com.eiu.capstone.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.CloneLabErrorDTO;
import com.eiu.capstone.backend.DTO.CloneLabRefDTO;
import com.eiu.capstone.backend.DTO.CloneLabsResponse;
import com.eiu.capstone.backend.DTO.CloneSourceTermDTO;
import com.eiu.capstone.backend.DTO.CloneSourcesResponse;
import com.eiu.capstone.backend.DTO.rubric.ChallengeStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ClassStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ConstructorStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.CreateLabRequest;
import com.eiu.capstone.backend.DTO.rubric.FieldStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.DTO.rubric.MethodStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ParameterStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.RelationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.AssertionStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InstanceStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.grading.rubric.RubricCacheInvalidationSupport;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.TermRepository;

@Service
public class LabCloneService {

    private static final Logger log = LoggerFactory.getLogger(LabCloneService.class);

    private final LabStructureService labStructureService;
    private final TestcaseRubricService testcaseRubricService;
    private final LabRepository labRepository;
    private final TermRepository termRepository;
    private final RubricCacheInvalidationSupport rubricCacheInvalidationSupport;
    private final TransactionTemplate requiresNewTx;

    public LabCloneService(LabStructureService labStructureService,
                           TestcaseRubricService testcaseRubricService,
                           LabRepository labRepository,
                           TermRepository termRepository,
                           RubricCacheInvalidationSupport rubricCacheInvalidationSupport,
                           PlatformTransactionManager transactionManager) {
        this.labStructureService = labStructureService;
        this.testcaseRubricService = testcaseRubricService;
        this.labRepository = labRepository;
        this.termRepository = termRepository;
        this.rubricCacheInvalidationSupport = rubricCacheInvalidationSupport;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public Optional<Term> findPreviousCurrentTerm() {
        List<Term> ordered = termRepository.findAllWithAcademicYear().stream()
                .sorted(Comparator
                        .comparing((Term t) -> t.getAcademicYear().getYearLabel()).reversed()
                        .thenComparing(Term::getTermNumber, Comparator.reverseOrder()))
                .toList();
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).isCurrent()) {
                if (i + 1 < ordered.size()) {
                    return Optional.of(ordered.get(i + 1));
                }
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public CloneSourcesResponse listCloneSources() {
        Optional<Term> previous = findPreviousCurrentTerm();
        if (previous.isEmpty()) {
            return new CloneSourcesResponse(null, List.of());
        }
        Term term = previous.get();
        CloneSourceTermDTO sourceTerm = new CloneSourceTermDTO(
                term.getId(),
                TermService.buildTermLabel(term),
                term.getAcademicYear() != null ? term.getAcademicYear().getYearLabel() : "",
                term.getTermNumber());
        List<CloneLabRefDTO> labs = labRepository.findByTerm_Id(term.getId()).stream()
                .sorted(Comparator.comparing(Lab::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(lab -> new CloneLabRefDTO(lab.getId(), lab.getName()))
                .toList();
        return new CloneSourcesResponse(sourceTerm, labs);
    }

    /**
     * Validates membership then clones each lab in its own short transaction (REQUIRES_NEW)
     * so Neon round-trips are not held inside one giant create-term transaction.
     */
    public CloneLabsResponse cloneLabs(List<UUID> sourceLabIds, UUID targetTermId, UUID allowedSourceTermId) {
        if (targetTermId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetTermId is required");
        }
        if (allowedSourceTermId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Allowed source term is required");
        }
        if (!termRepository.existsById(targetTermId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid targetTermId");
        }
        List<UUID> ids = sourceLabIds != null ? sourceLabIds : List.of();
        if (ids.isEmpty()) {
            return new CloneLabsResponse(List.of(), List.of());
        }
        requireLabsInTerm(ids, allowedSourceTermId);

        if (ids.size() == 1) {
            return cloneOneBestEffort(ids.get(0), targetTermId);
        }

        // Parallel short TXs — Neon RTT dominates; overlapping labs cuts wall-clock.
        int workers = Math.min(ids.size(), 4);
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            List<CloneLabRefDTO> created = Collections.synchronizedList(new ArrayList<>());
            List<CloneLabErrorDTO> errors = Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<Void>> futures = new ArrayList<>(ids.size());
            for (UUID sourceLabId : ids) {
                futures.add(CompletableFuture.runAsync(
                        () -> cloneOneInto(sourceLabId, targetTermId, created, errors),
                        pool));
            }
            try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            } catch (CompletionException ex) {
                Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                throw cause instanceof RuntimeException runtime
                        ? runtime
                        : new IllegalStateException(cause);
            }
            return new CloneLabsResponse(List.copyOf(created), List.copyOf(errors));
        } finally {
            pool.shutdownNow();
        }
    }

    private CloneLabsResponse cloneOneBestEffort(UUID sourceLabId, UUID targetTermId) {
        List<CloneLabRefDTO> created = new ArrayList<>();
        List<CloneLabErrorDTO> errors = new ArrayList<>();
        cloneOneInto(sourceLabId, targetTermId, created, errors);
        return new CloneLabsResponse(List.copyOf(created), List.copyOf(errors));
    }

    private void cloneOneInto(UUID sourceLabId,
                              UUID targetTermId,
                              List<CloneLabRefDTO> created,
                              List<CloneLabErrorDTO> errors) {
        try {
            // Load outside the write TX so @Transactional(readOnly) does not mark the
            // clone connection read-only (Neon then rejects every INSERT).
            LoadedCloneSource loaded = loadCloneSource(sourceLabId);
            LabStructureResponse cloned = requiresNewTx.execute(
                    status -> writeClonedLab(loaded, targetTermId));
            if (cloned != null) {
                created.add(new CloneLabRefDTO(cloned.id(), cloned.name()));
            }
        } catch (ResponseStatusException ex) {
            String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
            log.warn("Lab clone failed source={} targetTerm={}: {}", sourceLabId, targetTermId, message);
            errors.add(new CloneLabErrorDTO(sourceLabId, message));
        } catch (RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : "Clone failed";
            log.warn("Lab clone failed source={} targetTerm={}: {}", sourceLabId, targetTermId, message, ex);
            errors.add(new CloneLabErrorDTO(sourceLabId, message));
        }
    }

    @Transactional(readOnly = true)
    public void requireLabsInTerm(List<UUID> sourceLabIds, UUID allowedSourceTermId) {
        if (sourceLabIds != null && sourceLabIds.stream().anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceLabIds must not contain null");
        }
        List<UUID> ids = sourceLabIds != null
                ? sourceLabIds.stream().distinct().toList()
                : List.of();
        if (ids.isEmpty()) {
            return;
        }
        List<Lab> labs = labRepository.findAllByIdWithTerm(ids);
        if (labs.size() != ids.size()) {
            Set<UUID> found = new HashSet<>();
            for (Lab lab : labs) {
                found.add(lab.getId());
            }
            for (UUID id : ids) {
                if (!found.contains(id)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lab not found: " + id);
                }
            }
        }
        for (Lab lab : labs) {
            UUID termId = lab.getTerm() != null ? lab.getTerm().getId() : null;
            if (!Objects.equals(termId, allowedSourceTermId)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Lab " + lab.getId() + " is not in the allowed source quarter");
            }
        }
    }

    public LabStructureResponse cloneLab(UUID sourceLabId, UUID targetTermId) {
        LoadedCloneSource loaded = loadCloneSource(sourceLabId);
        return requiresNewTx.execute(status -> writeClonedLab(loaded, targetTermId));
    }

    private LoadedCloneSource loadCloneSource(UUID sourceLabId) {
        LabStructureResponse source = labStructureService.loadForEditor(sourceLabId);
        List<ChallengeStructureDTO> sourceChallenges =
                source.challenges() != null ? source.challenges() : List.of();
        List<UUID> sourceChallengeIds = sourceChallenges.stream()
                .map(ChallengeStructureDTO::id)
                .filter(Objects::nonNull)
                .toList();
        Map<UUID, List<TestcaseStructureDTO>> otByChallenge = sourceChallengeIds.isEmpty()
                ? Map.of()
                : testcaseRubricService.loadDtosGroupedByChallengeIds(sourceChallengeIds);
        return new LoadedCloneSource(source, sourceChallenges, otByChallenge);
    }

    private LabStructureResponse writeClonedLab(LoadedCloneSource loaded, UUID targetTermId) {
        LabStructureResponse created = labStructureService.createLab(
                new CreateLabRequest(loaded.source().name(), targetTermId, null));

        IdMap idMap = new IdMap();
        List<ChallengeStructureDTO> remappedChallenges = loaded.sourceChallenges().stream()
                .map(ch -> remapChallenge(ch, idMap))
                .toList();

        LabStructureResponse remappedStructure = new LabStructureResponse(
                created.id(),
                created.name(),
                created.termId(),
                created.deadlineDate(),
                created.studentVisible(),
                created.releaseDate(),
                remappedChallenges);
        labStructureService.saveLabStructureInsertOnly(created.id(), remappedStructure);

        Map<UUID, List<TestcaseStructureDTO>> remappedOtByNewChallenge = new LinkedHashMap<>();
        for (ChallengeStructureDTO sourceChallenge : loaded.sourceChallenges()) {
            if (sourceChallenge.id() == null) {
                continue;
            }
            List<TestcaseStructureDTO> remappedOt = loaded.otByChallenge()
                    .getOrDefault(sourceChallenge.id(), List.of())
                    .stream()
                    .map(tc -> remapTestcase(tc, idMap))
                    .toList();
            if (!remappedOt.isEmpty()) {
                remappedOtByNewChallenge.put(idMap.get(sourceChallenge.id()), remappedOt);
            }
        }
        if (!remappedOtByNewChallenge.isEmpty()) {
            testcaseRubricService.persistClonedTestcasesBatch(created.id(), remappedOtByNewChallenge);
        }
        rubricCacheInvalidationSupport.invalidateLab(created.id());
        return remappedStructure;
    }

    private record LoadedCloneSource(
            LabStructureResponse source,
            List<ChallengeStructureDTO> sourceChallenges,
            Map<UUID, List<TestcaseStructureDTO>> otByChallenge) {}

    private ChallengeStructureDTO remapChallenge(ChallengeStructureDTO source, IdMap idMap) {
        UUID newId = idMap.map(source.id());
        List<ClassStructureDTO> classes = source.classes() != null
                ? source.classes().stream().map(c -> remapClass(c, idMap)).toList()
                : List.of();
        List<RelationStructureDTO> relations = source.relations() != null
                ? source.relations().stream().map(r -> remapRelation(r, idMap)).toList()
                : List.of();
        return new ChallengeStructureDTO(
                newId,
                source.name(),
                source.challengeNumber(),
                classes,
                relations,
                source.hasMmd(),
                source.weight(),
                source.classWeight(),
                source.mmdWeight(),
                source.testcaseWeight());
    }

    private ClassStructureDTO remapClass(ClassStructureDTO source, IdMap idMap) {
        UUID newId = idMap.map(source.id());
        List<FieldStructureDTO> fields = source.fields() != null
                ? source.fields().stream().map(f -> remapField(f, idMap)).toList()
                : List.of();
        List<MethodStructureDTO> methods = source.methods() != null
                ? source.methods().stream().map(m -> remapMethod(m, idMap)).toList()
                : List.of();
        List<ConstructorStructureDTO> constructors = source.constructors() != null
                ? source.constructors().stream().map(c -> remapConstructor(c, idMap)).toList()
                : List.of();
        return new ClassStructureDTO(
                newId,
                source.name(),
                source.scopeId(),
                source.declaringTypeId(),
                source.isAbstract(),
                source.isStatic(),
                fields,
                methods,
                constructors,
                idMap.mapNullable(source.outerClassId()),
                source.weight());
    }

    private FieldStructureDTO remapField(FieldStructureDTO source, IdMap idMap) {
        return new FieldStructureDTO(idMap.map(source.id()), source.name(), source.dataType(), source.scopeId());
    }

    private MethodStructureDTO remapMethod(MethodStructureDTO source, IdMap idMap) {
        List<ParameterStructureDTO> parameters = source.parameters() != null
                ? source.parameters().stream().map(p -> remapParameter(p, idMap)).toList()
                : List.of();
        return new MethodStructureDTO(
                idMap.map(source.id()),
                source.name(),
                source.returnType(),
                source.scopeId(),
                source.isStatic(),
                source.isAbstract(),
                parameters);
    }

    private ConstructorStructureDTO remapConstructor(ConstructorStructureDTO source, IdMap idMap) {
        List<ParameterStructureDTO> parameters = source.parameters() != null
                ? source.parameters().stream().map(p -> remapParameter(p, idMap)).toList()
                : List.of();
        return new ConstructorStructureDTO(
                idMap.map(source.id()),
                source.name(),
                source.scopeId(),
                source.isDefault(),
                parameters);
    }

    private ParameterStructureDTO remapParameter(ParameterStructureDTO source, IdMap idMap) {
        return new ParameterStructureDTO(
                idMap.map(source.id()),
                source.name(),
                source.dataType(),
                source.orderIndex(),
                source.isFinal());
    }

    private RelationStructureDTO remapRelation(RelationStructureDTO source, IdMap idMap) {
        return new RelationStructureDTO(
                idMap.map(source.id()),
                idMap.mapNullable(source.sourceClassId()),
                idMap.mapNullable(source.targetClassId()),
                source.relationTypeId());
    }

    private TestcaseStructureDTO remapTestcase(TestcaseStructureDTO source, IdMap idMap) {
        List<InvocationStructureDTO> invocations = TestcaseRubricService.resolvedInvocations(source).stream()
                .map(inv -> remapInvocation(inv, idMap))
                .toList();
        List<AssertionStructureDTO> assertions = source.assertions() != null
                ? source.assertions().stream().map(a -> remapAssertion(a, idMap)).toList()
                : List.of();
        List<InstanceStructureDTO> instances = source.instances() != null
                ? source.instances().stream().map(i -> remapInstance(i, idMap)).toList()
                : List.of();
        InvocationStructureDTO singleInvocation = invocations.size() == 1 ? invocations.get(0) : null;
        return new TestcaseStructureDTO(
                idMap.map(source.id()),
                source.name(),
                source.testcaseType(),
                source.comparisonMethod(),
                source.weight(),
                source.orderIndex(),
                source.hidden(),
                singleInvocation,
                instances,
                assertions,
                invocations,
                source.oopPrincipleTag());
    }

    private InvocationStructureDTO remapInvocation(InvocationStructureDTO source, IdMap idMap) {
        return new InvocationStructureDTO(
                idMap.map(source.id()),
                source.invocationKind(),
                idMap.mapNullable(source.constructorId()),
                idMap.mapNullable(source.methodId()),
                source.params(),
                idMap.mapNullable(source.receiverConstructorId()),
                source.receiverParams(),
                source.instanceName(),
                idMap.mapNullable(source.dispatchClassId()));
    }

    private AssertionStructureDTO remapAssertion(AssertionStructureDTO source, IdMap idMap) {
        return new AssertionStructureDTO(
                idMap.map(source.id()),
                idMap.mapNullable(source.invocationId()),
                source.assertionKind(),
                idMap.mapNullable(source.fieldId()),
                source.expectedValue(),
                source.comparisonMode(),
                source.orderIndex());
    }

    private InstanceStructureDTO remapInstance(InstanceStructureDTO source, IdMap idMap) {
        return new InstanceStructureDTO(
                idMap.map(source.id()),
                source.label(),
                idMap.mapNullable(source.constructorId()),
                source.params());
    }

    private static final class IdMap {
        private final Map<UUID, UUID> map = new HashMap<>();

        UUID map(UUID oldId) {
            if (oldId == null) {
                return UUID.randomUUID();
            }
            return map.computeIfAbsent(oldId, ignored -> UUID.randomUUID());
        }

        UUID mapNullable(UUID oldId) {
            if (oldId == null) {
                return null;
            }
            return map(oldId);
        }

        UUID get(UUID oldId) {
            UUID mapped = map.get(oldId);
            if (mapped == null) {
                throw new IllegalStateException("Missing id mapping for " + oldId);
            }
            return mapped;
        }
    }
}
