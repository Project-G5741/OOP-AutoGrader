package com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.desktop.pack.DesktopPackLabMeta;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.ClassRubric;
import com.eiu.capstone.backend.grading.rubric.ConstructorRubric;
import com.eiu.capstone.backend.grading.rubric.FieldRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.grading.rubric.MethodRubric;
import com.eiu.capstone.backend.grading.rubric.ParameterRubric;

class DesktopPackStructureMapperTest {

    @Test
    void toStructure_preservesOrderedMemberAndParameterAliases() {
        DesktopMasterDataResolver masterData = mock(DesktopMasterDataResolver.class);
        when(masterData.scopeId(anyString())).thenReturn(1);
        when(masterData.declaringTypeId(anyString())).thenReturn(2);
        DesktopPackStructureMapper mapper = new DesktopPackStructureMapper(masterData);

        UUID labId = UUID.randomUUID();
        UUID challengeId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();
        UUID constructorId = UUID.randomUUID();
        ParameterRubric parameter = new ParameterRubric(
                List.of("amount", "quantity"),
                List.of("double", "float"));
        ClassRubric clazz = new ClassRubric(
                classId,
                "Invoice",
                null,
                "public",
                "class",
                false,
                false,
                List.of(new FieldRubric(
                        fieldId,
                        List.of("cost", "fee"),
                        "private",
                        List.of("double", "float"),
                        false,
                        false)),
                List.of(new MethodRubric(
                        methodId,
                        List.of("total", "sum"),
                        "public",
                        List.of("double", "float"),
                        false,
                        false,
                        false,
                        List.of(parameter))),
                List.of(new ConstructorRubric(
                        constructorId,
                        "public",
                        false,
                        List.of("double"),
                        List.of(parameter))),
                1);
        ChallengeRubric challenge = new ChallengeRubric(
                challengeId, 1, "Problem 1", List.of(clazz), List.of());
        LabRubricSnapshot snapshot = new LabRubricSnapshot(labId, Map.of(1, challenge));

        var structure = mapper.toStructure(
                new DesktopPackLabMeta(labId, "Aliases", true, null, null, "Practice"),
                snapshot,
                UUID.randomUUID());
        var mappedClass = structure.challenges().get(0).classes().get(0);

        assertEquals(List.of("cost", "fee"), mappedClass.fields().get(0).names());
        assertEquals(List.of("double", "float"), mappedClass.fields().get(0).dataTypes());
        assertEquals(List.of("total", "sum"), mappedClass.methods().get(0).names());
        assertEquals(List.of("double", "float"), mappedClass.methods().get(0).returnTypes());
        assertEquals(List.of("amount", "quantity"),
                mappedClass.methods().get(0).parameters().get(0).names());
        assertEquals(List.of("double", "float"),
                mappedClass.methods().get(0).parameters().get(0).dataTypes());
        assertEquals(List.of("amount", "quantity"),
                mappedClass.constructors().get(0).parameters().get(0).names());
        assertEquals(List.of("double", "float"),
                mappedClass.constructors().get(0).parameters().get(0).dataTypes());
    }
}
