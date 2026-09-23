package unit.com.eiu.capstone.backend.grading.rubric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.eiu.capstone.backend.DTO.rubric.testcase.AssertionStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.grading.rubric.InvocationRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubric;
import com.eiu.capstone.backend.grading.rubric.TestcaseRubricAssembler;
import com.eiu.capstone.backend.model.AssertionKind;
import com.eiu.capstone.backend.model.ClassEntity;
import com.eiu.capstone.backend.model.ComparisonMode;
import com.eiu.capstone.backend.model.Constructor;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.Method;
import com.eiu.capstone.backend.model.TestcaseType;
import com.eiu.capstone.backend.repository.ClassEntityRepository;
import com.eiu.capstone.backend.repository.ConstructorRepository;
import com.eiu.capstone.backend.repository.FieldRepository;
import com.eiu.capstone.backend.repository.MethodRepository;
import com.eiu.capstone.backend.repository.ParameterRepository;
import com.eiu.capstone.backend.service.TestcaseRubricService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TestcaseRubricAssemblerTest {

    @Mock private ClassEntityRepository classEntityRepository;
    @Mock private ConstructorRepository constructorRepository;
    @Mock private MethodRepository methodRepository;
    @Mock private FieldRepository fieldRepository;
    @Mock private ParameterRepository parameterRepository;
    @Mock private TestcaseRubricService testcaseRubricService;

    private TestcaseRubricAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new TestcaseRubricAssembler(
                classEntityRepository,
                constructorRepository,
                methodRepository,
                fieldRepository,
                parameterRepository,
                testcaseRubricService);
        doNothing().when(testcaseRubricService).validatePayload(any(), any());
    }

    @Test
    void assemble_compositionSteps_mapsInstanceNames() {
        UUID challengeId = UUID.randomUUID();
        UUID carClassId = UUID.randomUUID();
        UUID shapeClassId = UUID.randomUUID();
        UUID constructorId = UUID.randomUUID();
        UUID methodId = UUID.randomUUID();
        UUID ctorStepId = UUID.randomUUID();
        UUID methodStepId = UUID.randomUUID();
        UUID assertionId = UUID.randomUUID();

        ClassEntity car = new ClassEntity();
        car.setId(carClassId);
        car.setName("Car");
        ClassEntity shape = new ClassEntity();
        shape.setId(shapeClassId);
        shape.setName("Shape");

        Constructor constructor = new Constructor();
        constructor.setId(constructorId);
        constructor.setClassEntity(car);

        Method method = new Method();
        method.setId(methodId);
        method.setClassEntity(car);
        method.setName("getSpeed");

        when(classEntityRepository.findByChallenge_Id(challengeId)).thenReturn(List.of(car, shape));
        when(constructorRepository.findByClassEntityInWithDeclaration(any())).thenReturn(List.of(constructor));
        when(methodRepository.findByClassEntityInWithDeclaration(any())).thenReturn(List.of(method));
        when(fieldRepository.findByClassEntityInWithDeclaration(any())).thenReturn(List.of());
        when(parameterRepository.findByConstructorEntityIn(any())).thenReturn(List.of());
        when(parameterRepository.findByMethodIn(any())).thenReturn(List.of());

        InvocationStructureDTO construct = new InvocationStructureDTO(
                ctorStepId, InvocationKind.CONSTRUCTOR, constructorId, null, "[]", null, "[]", "car", null);
        InvocationStructureDTO call = new InvocationStructureDTO(
                methodStepId, InvocationKind.METHOD, null, methodId, "[]", constructorId, "[]", "car", shapeClassId);
        TestcaseStructureDTO dto = new TestcaseStructureDTO(
                UUID.randomUUID(),
                "sequence",
                TestcaseType.COMPOSITION,
                null,
                1,
                0,
                false,
                construct,
                List.of(),
                List.of(new AssertionStructureDTO(
                        assertionId, methodStepId, AssertionKind.RETURN_VALUE, null, "0", ComparisonMode.EXACT, 0)),
                List.of(construct, call),
                null);

        TestcaseRubric rubric = assembler.assemble(challengeId, dto);

        assertEquals(TestcaseType.COMPOSITION, rubric.testcaseType());
        assertEquals(2, rubric.invocations().size());
        InvocationRubric first = rubric.invocations().get(0);
        InvocationRubric second = rubric.invocations().get(1);
        assertEquals(ctorStepId, first.id());
        assertEquals("car", first.instanceName());
        assertEquals("Car", first.className());
        assertNull(first.dispatchClassId());
        assertEquals(methodStepId, second.id());
        assertEquals("car", second.instanceName());
        assertEquals(shapeClassId, second.dispatchClassId());
        assertEquals("Shape", second.dispatchClassName());
        assertEquals(ctorStepId, rubric.invocation().id());
        assertEquals(methodStepId, rubric.assertions().get(0).invocationId());
    }
}
