package com.eiu.capstone.backend.service.compile;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MemorySourceJavaFileObjectTest {

    @Test
    void getCharContent_roundTripsUtf8() {
        byte[] bytes = "public class Café {}".getBytes(StandardCharsets.UTF_8);
        MemorySourceJavaFileObject source = new MemorySourceJavaFileObject("Cafe.java", bytes);
        assertEquals("public class Café {}", source.getCharContent(true));
    }

    @Test
    void toSourceUri_encodesSpacesInPath() {
        URI uri = MemorySourceJavaFileObject.toSourceUri("model/Student File.java");
        assertEquals("string", uri.getScheme());
        assertEquals("/model/Student%20File.java", uri.getPath());
    }

    @Test
    void toSourceUri_rejectsEmptyPath() {
        assertThrows(IllegalArgumentException.class, () -> MemorySourceJavaFileObject.toSourceUri(""));
    }
}
