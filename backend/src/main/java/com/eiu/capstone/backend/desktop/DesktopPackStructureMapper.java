package com.eiu.capstone.backend.desktop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.eiu.capstone.backend.DTO.rubric.ChallengeStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ClassStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ConstructorStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.FieldStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.DTO.rubric.MethodStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.ParameterStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.RelationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.AssertionStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InstanceStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabMeta;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabEntry;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabMeta;
import com.eiu.capstone.backend.grading.rubric.AssertionRubric;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.InstanceRubric;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;

@Component
@Profile("desktop")
public class DesktopPackStructureMapper {

    private final DesktopMasterDataResolver masterData;

    public DesktopPackStructureMapper(DesktopMasterDataResolver masterData) {
        this.masterData = masterData;
    }

    public LabStructureResponse toStructure(DesktopPackLabMeta meta, LabRubricSnapshot rubric, UUID termId) {
        List<ChallengeStructureDTO> challenges = rubric.byChallengeNumber().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toChallenge(entry.getValue()))
                .toList();
        return new LabStructureResponse(
                meta.id(),
                meta.name(),
                termId,
                meta.deadlineDate(),
                meta.studentVisible(),
                meta.releaseDate(),
                challenges);
    }

    public Map<UUID, List<TestcaseStructureDTO>> testcasesByChallenge(LabRubricSnapshot rubric) {
        Map<UUID, List<TestcaseStructureDTO>> out = new HashMap<>();
        for (ChallengeRubric challenge : rubric.byChallengeNumber().values()) {
            List<TestcaseStructureDTO> rows = challenge.testcases() == null
                    ? List.of()
                    : challenge.testcases().stream()
                            .sorted(Comparator.comparingInt(TestcaseRubric::orderIndex))
                            .map(this::toTestcase)
                            .toList();
            if (!rows.isEmpty()) {
                out.put(challenge.challengeId(), rows);
            }
        }
        return out;
    }

    private ChallengeStructureDTO toChallenge(ChallengeRubric challenge) {
        Map<String, UUID> classIdByName = new HashMap<>();
        for (ClassRubric cls : challenge.classes()) {
            classIdByName.put(cls.name(), cls.id());
            if (cls.outerClassName() != null && !cls.outerClassName().isBlank()) {
                classIdByName.putIfAbsent(cls.outerClassName(), null);
            }
        }
        List<ClassStructureDTO> classes = challenge.classes().stream()
                .map(cls -> toClass(cls, classIdByName))
                .toList();
        List<RelationStructureDTO> relations = challenge.relations() == null
                ? List.of()
                : challenge.relations().stream().map(this::toRelation).toList();
        return new ChallengeStructureDTO(
                challenge.challengeId(),
                challenge.name(),
                challenge.challengeNumber(),
                classes,
                relations,
                challenge.hasMmd(),
                challenge.weight(),
                challenge.classWeight(),
                challenge.mmdWeight(),
                challenge.testcaseWeight());
    }

    private ClassStructureDTO toClass(ClassRubric cls, Map<String, UUID> classIdByName) {
        UUID outerClassId = cls.outerClassName() != null && !cls.outerClassName().isBlank()
                ? classIdByName.get(cls.outerClassName())
                : null;
        List<FieldStructureDTO> fields = cls.fields() == null ? List.of() : cls.fields().stream()
                .map(this::toField)
                .toList();
        List<MethodStructureDTO> methods = cls.methods() == null ? List.of() : cls.methods().stream()
                .map(this::toMethod)
                .toList();
        List<ConstructorStructureDTO> constructors = cls.constructors() == null ? List.of()
                : cls.constructors().stream().map(this::toConstructor).toList();
        return new ClassStructureDTO(
                cls.id(),
                cls.name(),
                masterData.scopeId(cls.scope()),
                masterData.declaringTypeId(cls.declaringType()),
                cls.isAbstract(),
                cls.isStatic(),
                fields,
                methods,
                constructors,
                outerClassId,
                cls.weight());
    }

    private FieldStructureDTO toField(FieldRubric field) {
        return new FieldStructureDTO(
                field.id(),
                field.name(),
                field.dataType(),
                masterData.scopeId(field.scope()));
    }

    private MethodStructureDTO toMethod(MethodRubric method) {
        List<ParameterStructureDTO> parameters = new ArrayList<>();
        List<String> types = method.parameterTypes() == null ? List.of() : method.parameterTypes();
        for (int i = 0; i < types.size(); i++) {
            parameters.add(new ParameterStructureDTO(
                    UUID.randomUUID(),
                    "arg" + i,
                    types.get(i),
                    i,
                    method.isFinal()));
        }
        return new MethodStructureDTO(
                method.id(),
                method.name(),
                method.returnType(),
                masterData.scopeId(method.scope()),
                method.isStatic(),
                method.isAbstract(),
                parameters);
    }

    private ConstructorStructureDTO toConstructor(ConstructorRubric constructor) {
        List<ParameterStructureDTO> parameters = new ArrayList<>();
        List<String> types = constructor.parameterTypes() == null ? List.of() : constructor.parameterTypes();
        for (int i = 0; i < types.size(); i++) {
            parameters.add(new ParameterStructureDTO(
                    UUID.randomUUID(),
                    "arg" + i,
                    types.get(i),
                    i,
                    false));
        }
        return new ConstructorStructureDTO(
                constructor.id(),
                constructor.isDefault() ? "default" : "constructor",
                masterData.scopeId(constructor.scope()),
                constructor.isDefault(),
                parameters);
    }

    private RelationStructureDTO toRelation(RelationRubric relation) {
        return new RelationStructureDTO(
                relation.id(),
                relation.sourceClassId(),
                relation.targetClassId(),
                masterData.relationTypeId(relation.relationTypeName()));
    }

    private TestcaseStructureDTO toTestcase(TestcaseRubric testcase) {
        List<InvocationStructureDTO> invocations = testcase.invocations() == null
                ? List.of()
                : testcase.invocations().stream().map(this::toInvocation).toList();
        InvocationStructureDTO singular = invocations.isEmpty() ? null : invocations.get(0);
        List<InstanceStructureDTO> instances = testcase.instances() == null ? List.of()
                : testcase.instances().stream()
                        .map(inst -> new InstanceStructureDTO(
                                inst.id(), inst.label(), inst.constructorId(), inst.paramsJson()))
                        .toList();
        List<AssertionStructureDTO> assertions = testcase.assertions() == null ? List.of()
                : testcase.assertions().stream().map(this::toAssertion).toList();
        return new TestcaseStructureDTO(
                testcase.id(),
                testcase.name(),
                testcase.testcaseType(),
                testcase.comparisonMethod(),
                testcase.weight(),
                testcase.orderIndex(),
                testcase.hidden(),
                singular,
                instances,
                assertions,
                invocations,
                testcase.oopPrincipleTag());
    }

    private InvocationStructureDTO toInvocation(InvocationRubric invocation) {
        return new InvocationStructureDTO(
                invocation.id(),
                invocation.kind(),
                invocation.constructorId(),
                invocation.methodId(),
                invocation.paramsJson(),
                invocation.receiverConstructorId(),
                invocation.receiverParamsJson(),
                invocation.instanceName(),
                invocation.dispatchClassId());
    }

    private AssertionStructureDTO toAssertion(AssertionRubric assertion) {
        return new AssertionStructureDTO(
                assertion.id(),
                assertion.invocationId(),
                assertion.kind(),
                assertion.fieldId(),
                assertion.expectedValueJson(),
                assertion.comparisonMode(),
                assertion.orderIndex());
    }
}
