package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.DTO.ClassConstructorDetailDTO;
import com.eiu.capstone.backend.DTO.ClassDetailDTO;
import com.eiu.capstone.backend.DTO.ClassFieldDetailDTO;
import com.eiu.capstone.backend.DTO.ClassMethodDetailDTO;
import com.eiu.capstone.backend.DTO.MmdClassDTO;
import com.eiu.capstone.backend.DTO.MmdRelationDTO;
import com.eiu.capstone.backend.grading.MmdGradingOutcome;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ChallengeSnapshot;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;
import com.eiu.capstone.backend.service.ChallengeCompileErrors;
import com.eiu.capstone.backend.service.ClassStructureService;
import com.eiu.capstone.backend.service.DisclosureMode;
import com.eiu.capstone.backend.service.StudentDisplayMessages;
import com.eiu.capstone.backend.service.SubmissionCorrectIds;
import com.eiu.capstone.backend.service.SubmissionMmdMetaStore.ChallengeMmdMeta;

class ClassStructureServiceDisclosureTest {

    private ClassStructureService service;

    @BeforeEach
    void setUp() {
        service = new ClassStructureService(
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false);
    }

    @Test
    void studentMode_missingClassMembers_useGenericLabelsNotRubric() {
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();

        ClassRubric classRubric = new ClassRubric(
                classId,
                "Person",
                "PRIVATE",
                "CLASS",
                false,
                List.of(new FieldRubric(fieldId, "age", "PRIVATE", "int", false, false)),
                List.of(),
                List.of());
        ChallengeRubric challengeRubric = new ChallengeRubric(
                UUID.randomUUID(), 1, "Challenge 1", List.of(classRubric), List.of());

        List<ClassDetailDTO> result = service.buildClassDataFromRubric(
                challengeRubric,
                new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of()),
                ChallengeCompileErrors.none(),
                new ChallengeSnapshot(),
                DisclosureMode.STUDENT);

        ClassFieldDetailDTO field = result.get(0).fields().get(0);
        assertEquals(StudentDisplayMessages.MISSING_VARIABLE, field.name());
        assertEquals(StudentDisplayMessages.PLACEHOLDER, field.scope());
        assertEquals(StudentDisplayMessages.PLACEHOLDER, field.dataType());
        assertFalse(field.ok());
    }

    @Test
    void studentMode_wrongClassField_showsWrongVariableMessage() {
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();

        ClassRubric classRubric = new ClassRubric(
                classId,
                "Person",
                "PRIVATE",
                "CLASS",
                false,
                List.of(new FieldRubric(fieldId, "age", "PRIVATE", "int", false, false)),
                List.of(),
                List.of());
        ChallengeRubric challengeRubric = new ChallengeRubric(
                UUID.randomUUID(), 1, "Challenge 1", List.of(classRubric), List.of());

        var classSnapshot = new com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassSnapshot();
        var entry = new com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ClassFieldEntry();
        entry.name = "name";
        entry.scope = "private";
        entry.dataType = "String";
        classSnapshot.fields.put(fieldId.toString(), entry);
        classSnapshot.fieldGrades.put(fieldId.toString(), "fail");

        ChallengeSnapshot snapshot = new ChallengeSnapshot();
        snapshot.classSnapshot = classSnapshot;

        List<ClassDetailDTO> result = service.buildClassDataFromRubric(
                challengeRubric,
                new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of()),
                ChallengeCompileErrors.none(),
                snapshot,
                DisclosureMode.STUDENT);

        ClassFieldDetailDTO field = result.get(0).fields().get(0);
        assertEquals(StudentDisplayMessages.WRONG_VARIABLE, field.name());
        assertFalse(field.ok());
    }

    @Test
    void lecturerMode_missingClassMembers_useGenericLabelsLikeStudent() {
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();

        ClassRubric classRubric = new ClassRubric(
                classId,
                "Person",
                "PRIVATE",
                "CLASS",
                false,
                List.of(new FieldRubric(fieldId, "age", "PRIVATE", "int", false, false)),
                List.of(),
                List.of());
        ChallengeRubric challengeRubric = new ChallengeRubric(
                UUID.randomUUID(), 1, "Challenge 1", List.of(classRubric), List.of());

        List<ClassDetailDTO> result = service.buildClassDataFromRubric(
                challengeRubric,
                new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of()),
                ChallengeCompileErrors.none(),
                new ChallengeSnapshot(),
                DisclosureMode.LECTURER);

        ClassFieldDetailDTO field = result.get(0).fields().get(0);
        assertEquals(StudentDisplayMessages.MISSING_VARIABLE, field.name());
        assertEquals(StudentDisplayMessages.PLACEHOLDER, field.scope());
        assertEquals(StudentDisplayMessages.PLACEHOLDER, field.dataType());
        assertFalse(field.ok());
    }

    @Test
    void correctMembersWithoutSnapshot_fallBackToRubricLabelsNotMissing() {
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        UUID constructorId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();

        ClassRubric classRubric = new ClassRubric(
                classId,
                "Employee",
                "PUBLIC",
                "CLASS",
                false,
                List.of(new FieldRubric(fieldId, "name", "PRIVATE", "String", false, false)),
                List.of(new MethodRubric(
                        methodId, "getName", "PUBLIC", "String", false, false, false, List.of())),
                List.of(new ConstructorRubric(constructorId, "PUBLIC", true, List.of())));
        ChallengeRubric challengeRubric = new ChallengeRubric(
                UUID.randomUUID(), 0, "Challenge 0", List.of(classRubric), List.of());

        List<ClassDetailDTO> result = service.buildClassDataFromRubric(
                challengeRubric,
                new SubmissionCorrectIds(Set.of(fieldId), Set.of(methodId), Set.of(constructorId), Set.of()),
                ChallengeCompileErrors.none(),
                new ChallengeSnapshot(),
                DisclosureMode.STUDENT);

        ClassDetailDTO cls = result.get(0);
        ClassFieldDetailDTO field = cls.fields().get(0);
        assertTrue(field.ok());
        assertEquals("name", field.name());
        assertEquals("PRIVATE", field.scope());
        assertEquals("String", field.dataType());

        ClassConstructorDetailDTO constructor = cls.constructors().get(0);
        assertTrue(constructor.ok());
        assertEquals("Employee", constructor.name());
        assertEquals("PUBLIC", constructor.scope());
        assertEquals("", constructor.params());

        ClassMethodDetailDTO method = cls.methods().get(0);
        assertTrue(method.ok());
        assertEquals("getName", method.name());
        assertEquals("public", method.scope());
        assertEquals("String", method.returnType());
    }

    @Test
    void mmdOutcome_overridesClassCorrectIds_forFailedMember() {
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();

        ClassRubric classRubric = new ClassRubric(
                classId,
                "Employee",
                "PUBLIC",
                "CLASS",
                false,
                List.of(new FieldRubric(fieldId, "name", "PRIVATE", "String", false, false)),
                List.of(),
                List.of());
        ChallengeRubric challengeRubric = new ChallengeRubric(
                UUID.randomUUID(), 0, "Challenge 0", List.of(classRubric), List.of());

        MmdGradingOutcome mmdOutcome = new MmdGradingOutcome();
        mmdOutcome.setClassPresent(classId, true);
        mmdOutcome.setClass(classId, true);
        mmdOutcome.setField(fieldId, false);

        List<MmdClassDTO> result = service.buildMmdDataFromRubric(
                challengeRubric,
                new SubmissionCorrectIds(Set.of(fieldId), Set.of(), Set.of(), Set.of()),
                mmdOutcome,
                true,
                new ChallengeMmdMeta(),
                UUID.randomUUID(),
                new ChallengeSnapshot(),
                DisclosureMode.STUDENT);

        var fieldAttr = result.get(0).attributes().stream()
                .filter(a -> "field".equals(a.type()))
                .findFirst()
                .orElseThrow();
        assertFalse(fieldAttr.ok());
    }

    @Test
    void studentMode_missingMmdRelation_doesNotRevealRubricEndpoints() {
        UUID classId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();

        ClassRubric classRubric = new ClassRubric(
                classId, "Car", "PUBLIC", "CLASS", false, List.of(), List.of(), List.of());
        RelationRubric relation = new RelationRubric(
                relationId, classId, "Car", UUID.randomUUID(), "Vehicle", "GENERALIZATION");
        ChallengeRubric challengeRubric = new ChallengeRubric(
                UUID.randomUUID(), 1, "Challenge 1", List.of(classRubric), List.of(relation));

        List<MmdClassDTO> result = service.buildMmdDataFromRubric(
                challengeRubric,
                new SubmissionCorrectIds(Set.of(), Set.of(), Set.of(), Set.of()),
                null,
                false,
                new ChallengeMmdMeta(),
                UUID.randomUUID(),
                new ChallengeSnapshot(),
                DisclosureMode.STUDENT);

        MmdRelationDTO relationDto = result.get(0).relations().get(0);
        assertEquals(StudentDisplayMessages.PLACEHOLDER, relationDto.from());
        assertEquals(StudentDisplayMessages.PLACEHOLDER, relationDto.to());
        assertEquals(StudentDisplayMessages.PLACEHOLDER, relationDto.relType());
        assertEquals(StudentDisplayMessages.REQUIRED_RELATIONSHIP, relationDto.error());
    }
}
