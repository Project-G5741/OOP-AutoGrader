package unit.com.eiu.capstone.backend.desktop.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.desktop.pack.DesktopPackFileNames;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFileNames.PackKind;
import com.eiu.capstone.backend.desktop.pack.DesktopPackInnerPayload;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabEntry;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabMeta;
import com.eiu.capstone.backend.desktop.pack.DesktopPackManifest;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;

class DesktopPackFileNamesTest {

    private static final UUID TERM_ID = UUID.fromString("412ffd13-b720-4d24-85c3-6b0b705137f8");
    private static final UUID LAB_ID = UUID.fromString("23c0240f-2b34-46ef-86ef-87cf330c6ea1");

    @Test
    void parseAcceptsTermAndLabNames() {
        var term = DesktopPackFileNames.parse("Rubric_2026-2027_Q1.agpack");
        assertEquals(PackKind.TERM, term.kind());
        assertEquals("2026-2027", term.yearLabel());
        assertEquals(1, term.termNumber());

        var lab = DesktopPackFileNames.parse("Rubric_Lab 2.agpack");
        assertEquals(PackKind.LAB, lab.kind());
        assertEquals("Lab 2", lab.labName());
    }

    @Test
    void parseStripsWindowsPath() {
        var parsed = DesktopPackFileNames.parse("C:\\Users\\PC\\Downloads\\Rubric_Midterm.agpack");
        assertEquals(PackKind.LAB, parsed.kind());
        assertEquals("Midterm", parsed.labName());
    }

    @Test
    void parseRejectsLegacyUuidNames() {
        assertThrows(ResponseStatusException.class,
                () -> DesktopPackFileNames.parse("412ffd13-b720-4d24-85c3-6b0b705137f8.term.agpack"));
        assertThrows(ResponseStatusException.class,
                () -> DesktopPackFileNames.parse("23c0240f-2b34-46ef-86ef-87cf330c6ea1.lab.agpack"));
        assertThrows(ResponseStatusException.class,
                () -> DesktopPackFileNames.parse("Term_2026-2027_Q1.agpack"));
        assertThrows(ResponseStatusException.class,
                () -> DesktopPackFileNames.parse("Lab_Midterm.agpack"));
    }

    @Test
    void parseRejectsLabNameOver50() {
        String longName = "x".repeat(51);
        assertThrows(ResponseStatusException.class,
                () -> DesktopPackFileNames.parse("Rubric_" + longName + ".agpack"));
    }

    @Test
    void assertMatchesPayloadRejectsRenamedLabPack() {
        var parsed = DesktopPackFileNames.parse("Rubric_Other.agpack");
        DesktopPackInnerPayload inner = sampleLabInner("Midterm");
        DesktopPackManifest manifest = sampleLabManifest(inner);
        assertThrows(ResponseStatusException.class,
                () -> DesktopPackFileNames.assertMatchesPayload(parsed, manifest, inner));
    }

    @Test
    void assertMatchesPayloadAcceptsMatchingTermPack() {
        var parsed = DesktopPackFileNames.parse("Rubric_2026_Q1.agpack");
        DesktopPackInnerPayload inner = sampleTermInner("2026 — Quarter 1");
        DesktopPackManifest manifest = sampleTermManifest(inner);
        DesktopPackFileNames.assertMatchesPayload(parsed, manifest, inner);
    }

    private static DesktopPackInnerPayload sampleLabInner(String labName) {
        return new DesktopPackInnerPayload(
                "v1",
                TERM_ID,
                "2026 — Quarter 1",
                List.of(new DesktopPackLabEntry(
                        new DesktopPackLabMeta(LAB_ID, labName, true, null, null),
                        new LabRubricSnapshot(LAB_ID, Map.of()))));
    }

    private static DesktopPackManifest sampleLabManifest(DesktopPackInnerPayload inner) {
        return new DesktopPackManifest(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                inner.packVersion(),
                inner.termId(),
                inner.termLabel(),
                Instant.parse("2026-09-01T00:00:00Z"),
                List.of(LAB_ID),
                "placeholder");
    }

    private static DesktopPackInnerPayload sampleTermInner(String termLabel) {
        return new DesktopPackInnerPayload(
                "v1",
                TERM_ID,
                termLabel,
                List.of(new DesktopPackLabEntry(
                        new DesktopPackLabMeta(LAB_ID, "Bootstrap Lab", true, null, null),
                        new LabRubricSnapshot(LAB_ID, Map.of()))));
    }

    private static DesktopPackManifest sampleTermManifest(DesktopPackInnerPayload inner) {
        return new DesktopPackManifest(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                inner.packVersion(),
                inner.termId(),
                inner.termLabel(),
                Instant.parse("2026-09-01T00:00:00Z"),
                List.of(LAB_ID),
                "placeholder");
    }
}
