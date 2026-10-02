package unit.com.eiu.capstone.backend.desktop.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.eiu.capstone.backend.desktop.pack.DesktopPackFileNames;
import com.eiu.capstone.backend.desktop.pack.DesktopPackFileNames.PackKind;

class DesktopPackFileNamesTest {

    @Test
    void parseAcceptsCanonicalNames() {
        var parsed = DesktopPackFileNames.parse("412ffd13-b720-4d24-85c3-6b0b705137f8.term.agpack");
        assertEquals(PackKind.TERM, parsed.kind());
        assertEquals("412ffd13-b720-4d24-85c3-6b0b705137f8.term.agpack", parsed.filename());
    }

    @Test
    void parseStripsWindowsPath() {
        var parsed = DesktopPackFileNames.parse(
                "C:\\Users\\PC\\Downloads\\23c0240f-2b34-46ef-86ef-87cf330c6ea1.lab.agpack");
        assertEquals(PackKind.LAB, parsed.kind());
    }
}
