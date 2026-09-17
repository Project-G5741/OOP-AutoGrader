package unit.com.eiu.capstone.backend.DTO.rubric.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.model.InvocationKind;
import com.eiu.capstone.backend.model.OopPrincipleTag;
import com.eiu.capstone.backend.model.TestcaseComparisonMethod;
import com.eiu.capstone.backend.model.TestcaseType;
import com.fasterxml.jackson.databind.json.JsonMapper;

class TestcaseStructureDTOParseTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void twoOrderedInvocations_roundTripInstanceNameAndDispatchClassId() throws Exception {
        UUID firstId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID secondId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID constructorId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID methodId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        UUID dispatchClassId = UUID.fromString("55555555-5555-5555-5555-555555555555");

        String json = """
                {
                  "name": "area-of-shape",
                  "testcaseType": "SINGLE_INVOCATION",
                  "oopPrincipleTag": "Polymorphism",
                  "weight": 1,
                  "orderIndex": 0,
                  "hidden": false,
                  "invocations": [
                    {
                      "id": "%s",
                      "invocationKind": "CONSTRUCTOR",
                      "constructorId": "%s",
                      "params": "[]",
                      "receiverParams": "[]",
                      "instanceName": "shape",
                      "dispatchClassId": null
                    },
                    {
                      "id": "%s",
                      "invocationKind": "METHOD",
                      "methodId": "%s",
                      "params": "[]",
                      "receiverParams": "[]",
                      "instanceName": "shape",
                      "dispatchClassId": "%s"
                    }
                  ]
                }
                """.formatted(firstId, constructorId, secondId, methodId, dispatchClassId);

        TestcaseStructureDTO parsed = mapper.readValue(json, TestcaseStructureDTO.class);

        assertEquals(TestcaseType.SINGLE_INVOCATION, parsed.testcaseType());
        assertEquals(OopPrincipleTag.Polymorphism, parsed.oopPrincipleTag());
        List<InvocationStructureDTO> steps = parsed.invocations();
        assertEquals(2, steps.size());
        assertEquals(firstId, steps.get(0).id());
        assertEquals(InvocationKind.CONSTRUCTOR, steps.get(0).invocationKind());
        assertEquals("shape", steps.get(0).instanceName());
        assertNull(steps.get(0).dispatchClassId());
        assertEquals(secondId, steps.get(1).id());
        assertEquals(InvocationKind.METHOD, steps.get(1).invocationKind());
        assertEquals("shape", steps.get(1).instanceName());
        assertEquals(dispatchClassId, steps.get(1).dispatchClassId());

        TestcaseStructureDTO roundTrip = mapper.readValue(
                mapper.writeValueAsString(parsed), TestcaseStructureDTO.class);
        assertEquals(parsed.oopPrincipleTag(), roundTrip.oopPrincipleTag());
        assertEquals(parsed.invocations().get(0).instanceName(),
                roundTrip.invocations().get(0).instanceName());
        assertEquals(parsed.invocations().get(1).dispatchClassId(),
                roundTrip.invocations().get(1).dispatchClassId());
    }

    @Test
    void legacySingularInvocationJson_stillParsesWithoutTagOrInvocationsList() throws Exception {
        String json = """
                {
                  "name": "deposit",
                  "testcaseType": "SINGLE_INVOCATION",
                  "weight": 1,
                  "orderIndex": 0,
                  "hidden": false,
                  "invocation": {
                    "id": "11111111-1111-1111-1111-111111111111",
                    "invocationKind": "METHOD",
                    "methodId": "44444444-4444-4444-4444-444444444444",
                    "params": "[]",
                    "receiverParams": "[]"
                  }
                }
                """;

        TestcaseStructureDTO parsed = mapper.readValue(json, TestcaseStructureDTO.class);

        assertEquals(TestcaseType.SINGLE_INVOCATION, parsed.testcaseType());
        assertEquals("11111111-1111-1111-1111-111111111111", parsed.invocation().id().toString());
        assertNull(parsed.invocations());
        assertNull(parsed.oopPrincipleTag());
        assertNull(parsed.invocation().instanceName());
        assertNull(parsed.invocation().dispatchClassId());
    }

    @Test
    void comparisonPayload_ignoresNewInvocationColumns() throws Exception {
        String json = """
                {
                  "name": "compare",
                  "testcaseType": "COMPARISON",
                  "comparisonMethod": "EQUALS",
                  "weight": 1,
                  "orderIndex": 0,
                  "hidden": false,
                  "instances": [
                    {"id": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", "label": "A",
                     "constructorId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "params": "[]"},
                    {"id": "cccccccc-cccc-cccc-cccc-cccccccccccc", "label": "B",
                     "constructorId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "params": "[]"}
                  ],
                  "assertions": []
                }
                """;

        TestcaseStructureDTO parsed = mapper.readValue(json, TestcaseStructureDTO.class);

        assertEquals(TestcaseType.COMPARISON, parsed.testcaseType());
        assertEquals(TestcaseComparisonMethod.EQUALS, parsed.comparisonMethod());
        assertNull(parsed.invocation());
        assertNull(parsed.invocations());
        assertNull(parsed.oopPrincipleTag());
        assertEquals(2, parsed.instances().size());
    }
}
