package com.eiu.capstone.backend.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.grading.ParsedSubmissionSnapshot.ChallengeSnapshot;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.RelationRubric;

class ParsedSubmissionSnapshotBuilderTest {

    private final ParsedSubmissionSnapshotBuilder builder = new ParsedSubmissionSnapshotBuilder();

    @Test
    void classField_sameNameWrongType_capturesStudentDisplay() {
        UUID fieldId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(new ClassRubric(
                        classId,
                        "Person",
                        "public",
                        "CLASS",
                        false,
                        List.of(new FieldRubric(fieldId, "age", "private", "int")),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        ParsedClass parsedClass = new ParsedClass();
        parsedClass.simpleName = "Person";
        ParsedField studentField = new ParsedField();
        studentField.name = "age";
        studentField.scope = "private";
        studentField.dataType = "String";
        parsedClass.fields = List.of(studentField);
        parsedClass.methods = List.of();
        parsedClass.constructors = List.of();

        ChallengeSnapshot snapshot = builder.build(rubric, List.of(parsedClass), null);

        assertTrue(snapshot.classSnapshot.fields.containsKey(fieldId.toString()));
        var entry = snapshot.classSnapshot.fields.get(fieldId.toString());
        assertEquals("age", entry.name);
        assertEquals("String", entry.dataType);
    }

    @Test
    void classField_missing_doesNotCaptureEntry() {
        UUID fieldId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(new ClassRubric(
                        classId,
                        "Person",
                        "public",
                        "CLASS",
                        false,
                        List.of(new FieldRubric(fieldId, "age", "private", "int")),
                        List.of(),
                        List.of())),
                List.of(),
                List.of());

        ParsedClass parsedClass = new ParsedClass();
        parsedClass.simpleName = "Person";
        parsedClass.fields = List.of();
        parsedClass.methods = List.of();
        parsedClass.constructors = List.of();

        ChallengeSnapshot snapshot = builder.build(rubric, List.of(parsedClass), null);

        assertFalse(snapshot.classSnapshot.fields.containsKey(fieldId.toString()));
    }

    @Test
    void mmdRelation_wrongType_capturesStudentEndpoints() {
        UUID relationId = UUID.randomUUID();
        ChallengeRubric rubric = new ChallengeRubric(
                UUID.randomUUID(),
                1,
                "Challenge 1",
                List.of(),
                List.of(new RelationRubric(
                        relationId,
                        UUID.randomUUID(),
                        "Car",
                        UUID.randomUUID(),
                        "Vehicle",
                        "inheritance")),
                List.of());

        ParsedMmdRelation parsedRelation = new ParsedMmdRelation();
        parsedRelation.sourceClassName = "Car";
        parsedRelation.targetClassName = "Vehicle";
        parsedRelation.relationType = "association";

        ParsedMmdDiagram diagram = new ParsedMmdDiagram(List.of(), List.of(parsedRelation));

        ChallengeSnapshot snapshot = builder.build(rubric, List.of(), diagram);

        assertTrue(snapshot.mmdSnapshot.relations.containsKey(relationId.toString()));
        var entry = snapshot.mmdSnapshot.relations.get(relationId.toString());
        assertEquals("Car", entry.from);
        assertEquals("Vehicle", entry.to);
        assertEquals("association", entry.relType);
    }
}
