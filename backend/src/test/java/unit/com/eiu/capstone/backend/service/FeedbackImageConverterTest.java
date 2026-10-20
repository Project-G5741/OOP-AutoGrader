package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.feedback.ConvertedAttachment;
import com.eiu.capstone.backend.service.FeedbackImageConverter;

class FeedbackImageConverterTest {

    private final FeedbackImageConverter converter = new FeedbackImageConverter();

    @Test
    void pngBecomesWebp() throws Exception {
        ConvertedAttachment converted = converter.convertRasterToWebp("shot.png", pngBytes());
        assertEquals("image/webp", converted.storedContentType());
        assertEquals("shot.png", converted.originalFilename());
        assertTrue(converted.content().length > 0);
        assertTrue(isRiffWebp(converted.content()));
    }

    @Test
    void fakePngBytesAreRejected() {
        byte[] fake = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> converter.convertRasterToWebp("shot.png", fake));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
    }

    @Test
    void javaIsPassedThrough() {
        byte[] source = "class Car {}".getBytes();
        ConvertedAttachment converted = converter.passthroughSource("Car.java", "text/x-java-source", source);
        assertEquals("Car.java", converted.originalFilename());
        assertEquals("text/x-java-source", converted.storedContentType());
        assertEquals(source.length, converted.content().length);
    }

    private static byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static boolean isRiffWebp(byte[] bytes) {
        if (bytes.length < 12) {
            return false;
        }
        return bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
    }
}
