package com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class LabStructureAliasesOrPrimaryTest {

    @Test
    void aliasesOrPrimary_preservesNullTypeEntriesForEnumConstants() {
        UUID fieldDeclarationId = UUID.randomUUID();
        List<String> result = LabStructureService.aliasesOrPrimary(
                Map.of(fieldDeclarationId, Arrays.asList((String) null)),
                fieldDeclarationId,
                null);

        assertEquals(1, result.size());
        assertNull(result.get(0));
    }

    @Test
    void aliasesOrPrimary_fallsBackToPrimaryWhenAliasRowsMissing() {
        UUID fieldDeclarationId = UUID.randomUUID();
        assertEquals(
                List.of("speed"),
                LabStructureService.aliasesOrPrimary(Map.of(), fieldDeclarationId, "speed"));
    }
}
