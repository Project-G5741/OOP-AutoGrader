package support.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.DTO.ClassDetailDTO;
import com.eiu.capstone.backend.DTO.ClassFieldDetailDTO;
import com.eiu.capstone.backend.DTO.MmdClassDTO;
import com.eiu.capstone.backend.DTO.MmdRelationDTO;
import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ChallengeSnapshot;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
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
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false);
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
                List.of(new FieldRubric(fieldId, "age", "PRIVATE", "int")),
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
                List.of(new FieldRubric(fieldId, "age", "PRIVATE", "int")),
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
    void lecturerMode_missingClassMembers_keepRubricLabels() {
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();

        ClassRubric classRubric = new ClassRubric(
                classId,
                "Person",
                "PRIVATE",
                "CLASS",
                false,
                List.of(new FieldRubric(fieldId, "age", "PRIVATE", "int")),
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
        assertEquals("age", field.name());
        assertEquals("PRIVATE", field.scope());
        assertEquals("int", field.dataType());
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

    @Test
    void getTestcaseData_returnsEmptyWhileStudentOperationalTestsAreDark() {
        assertTrue(service.getTestcaseData(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).isEmpty());
        assertTrue(service.buildTestcaseDataForSubmission(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()).isEmpty());
    }
}
