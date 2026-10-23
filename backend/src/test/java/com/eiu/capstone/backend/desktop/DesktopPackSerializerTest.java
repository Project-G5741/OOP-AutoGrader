package com.eiu.capstone.backend.desktop;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.desktop.pack.DesktopPackCrypto;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFile;
import com.eiu.capstone.backend.desktop.pack.DesktopPackInnerPayload;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabEntry;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabMeta;
import com.eiu.capstone.backend.desktop.pack.DesktopPackManifest;
import com.eiu.capstone.backend.desktop.pack.DesktopPackSerializer;
import com.eiu.capstone.backend.grading.rubric.ChallengeRubric;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;

class DesktopPackSerializerTest {

    private static KeyPair keyPair;
    private static DesktopPackCrypto crypto;
    private static DesktopPackSerializer serializer;

    @BeforeAll
    static void setup() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("Ed25519");
        keyPair = gen.generateKeyPair();
        crypto = DesktopPackCrypto.withDefaultEmbeddedKey();
        serializer = new DesktopPackSerializer();
    }

    @Test
    void roundtrip_preserves_lab_and_challenge_ids() throws Exception {
        UUID termId = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        UUID challengeId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        LabRubricSnapshot rubric = new LabRubricSnapshot(
                labId,
                Map.of(1, new ChallengeRubric(challengeId, 1, "Main", List.of(), List.of())));
        DesktopPackLabMeta meta = new DesktopPackLabMeta(labId, "Lab 1", true, null, null, "Practice");
        DesktopPackInnerPayload payload = new DesktopPackInnerPayload(
                "pack-v1",
                termId,
                "2026 — Quarter 1",
                List.of(new DesktopPackLabEntry(meta, rubric)));
        byte[] json = serializer.toJson(payload);
        DesktopPackInnerPayload parsed = serializer.fromJson(json);
        assertEquals(1, parsed.labs().size());
        assertEquals(labId, parsed.labs().get(0).lab().id());
        assertEquals("Practice", parsed.labs().get(0).lab().prefix());
        assertEquals(challengeId, parsed.labs().get(0).rubric().byChallengeNumber().get(1).challengeId());

        DesktopPackManifest manifest = new DesktopPackManifest(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                "pack-v1",
                termId,
                "2026 — Quarter 1",
                Instant.parse("2026-09-01T00:00:00Z"),
                List.of(labId),
                "placeholder");
        DesktopPackFile pack = crypto.buildPack(keyPair.getPrivate(), manifest, json);
        byte[] opened = crypto.openPack(keyPair.getPublic(), pack);
        DesktopPackInnerPayload fromPack = serializer.fromJson(opened);
        assertEquals(challengeId, fromPack.labs().get(0).rubric().byChallengeNumber().get(1).challengeId());
        assertArrayEquals(json, serializer.toJson(fromPack));
    }

    @Test
    void omittedPrefix_readsAsOfficial() throws Exception {
        UUID labId = UUID.randomUUID();
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        String json = "{\"id\":\"" + labId + "\",\"name\":\"Lab 1\",\"studentVisible\":true}";
        DesktopPackLabMeta meta = mapper.readValue(json, DesktopPackLabMeta.class);
        assertEquals("Official", meta.prefix());
    }
}
