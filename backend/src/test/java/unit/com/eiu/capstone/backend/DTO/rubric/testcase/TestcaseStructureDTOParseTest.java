package unit.com.eiu.capstone.backend.DTO.rubric.testcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.DTO.rubric.testcase.InvocationStructureDTO;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.model.InvocationKind;
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
                  "testcaseType": "COMPOSITION",
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

        assertEquals(TestcaseType.COMPOSITION, parsed.testcaseType());
        assertNull(parsed.oopPrincipleTag());
        assertNull(parsed.comparisonMethod());
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
        assertEquals(parsed.testcaseType(), roundTrip.testcaseType());
        assertEquals(parsed.invocations().get(0).instanceName(),
                roundTrip.invocations().get(0).instanceName());
        assertEquals(parsed.invocations().get(1).dispatchClassId(),
                roundTrip.invocations().get(1).dispatchClassId());
    }

    @Test
    void unitJsonWithoutDroppedColumns_parsesAndIgnoresUnknownFields() throws Exception {
        String json = """
                {
                  "name": "deposit",
                  "testcaseType": "UNIT",
                  "hidden": true,
                  "legacyTag": "Polymorphism",
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

        assertEquals(TestcaseType.UNIT, parsed.testcaseType());
        assertEquals("11111111-1111-1111-1111-111111111111", parsed.invocation().id().toString());
        assertNull(parsed.invocations());
        assertNull(parsed.oopPrincipleTag());
        assertNull(parsed.comparisonMethod());
        assertTrue(parsed.hidden());
    }

    @Test
    void legacySingularInvocationJson_stillParsesWithoutTagOrInvocationsList() throws Exception {
        String json = """
                {
                  "name": "deposit",
                  "testcaseType": "UNIT",
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

        assertEquals(TestcaseType.UNIT, parsed.testcaseType());
        assertEquals("11111111-1111-1111-1111-111111111111", parsed.invocation().id().toString());
        assertNull(parsed.invocations());
        assertNull(parsed.oopPrincipleTag());
        assertNull(parsed.invocation().instanceName());
        assertNull(parsed.invocation().dispatchClassId());
    }
}
