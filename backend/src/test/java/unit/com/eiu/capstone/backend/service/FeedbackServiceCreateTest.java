package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.feedback.ConvertedAttachment;
import com.eiu.capstone.backend.DTO.feedback.FeedbackCreatedDTO;
import com.eiu.capstone.backend.model.FeedbackItem;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.FeedbackAttachmentRepository;
import com.eiu.capstone.backend.repository.FeedbackItemRepository;
import com.eiu.capstone.backend.service.FeedbackImageConverter;
import com.eiu.capstone.backend.service.FeedbackService;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceCreateTest {

    @Mock
    private FeedbackItemRepository itemRepository;
    @Mock
    private FeedbackAttachmentRepository attachmentRepository;
    @Mock
    private FeedbackImageConverter imageConverter;

    private FeedbackService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackService(itemRepository, attachmentRepository, imageConverter);
        lenient().when(itemRepository.save(any(FeedbackItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void stampsIrnAndNameFromAccountNotMultipart() {
        UserAccount user = account("2331200001", "Nguyen Van A");
        FeedbackCreatedDTO created = service.create(user, "CONTRIBUTION", "Please add dark mode", null);
        assertEquals("CONTRIBUTION", created.itemType());
        assertEquals("OPEN", created.status());

        ArgumentCaptor<FeedbackItem> captor = ArgumentCaptor.forClass(FeedbackItem.class);
        verify(itemRepository).save(captor.capture());
        FeedbackItem saved = captor.getValue();
        assertEquals("2331200001", saved.getAuthorIrn());
        assertEquals("Nguyen Van A", saved.getAuthorName());
        assertEquals("Please add dark mode", saved.getMessage());
    }

    @Test
    void pngAndJavaPersistWebpAndOriginalJava() {
        UserAccount user = account("2331200001", "Nguyen Van A");
        byte[] pngBytes = new byte[] { 1, 2, 3 };
        byte[] javaBytes = "class Car {}".getBytes();
        MockMultipartFile png = new MockMultipartFile("files", "shot.png", "image/png", pngBytes);
        MockMultipartFile java = new MockMultipartFile("files", "Car.java", "text/x-java-source", javaBytes);
        when(imageConverter.convertRasterToWebp("shot.png", pngBytes))
                .thenReturn(new ConvertedAttachment("shot.png", "image/webp", new byte[] { 9, 9 }));
        when(imageConverter.passthroughSource("Car.java", "text/x-java-source", javaBytes))
                .thenReturn(new ConvertedAttachment("Car.java", "text/x-java-source", javaBytes));

        service.create(user, "BUG", "Screenshot plus source", new MultipartFile[] { png, java });

        ArgumentCaptor<FeedbackItem> captor = ArgumentCaptor.forClass(FeedbackItem.class);
        verify(itemRepository).save(captor.capture());
        FeedbackItem saved = captor.getValue();
        assertEquals(2, saved.getAttachments().size());
        assertEquals("image/webp", saved.getAttachments().get(0).getStoredContentType());
        assertEquals("Car.java", saved.getAttachments().get(1).getOriginalFilename());
        assertEquals("text/x-java-source", saved.getAttachments().get(1).getStoredContentType());
    }

    @Test
    void pdfIsRejected() {
        UserAccount user = account("2331200001", "Nguyen Van A");
        MockMultipartFile pdf = new MockMultipartFile("files", "notes.pdf", "application/pdf", new byte[] { 1 });
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(user, "BUG", "See notes", new MultipartFile[] { pdf }));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void blankMessageIsRejected() {
        UserAccount user = account("2331200001", "Nguyen Van A");
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(user, "BUG", "   ", null));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void missingTypeIsRejected() {
        UserAccount user = account("2331200001", "Nguyen Van A");
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(user, "", "Hello", null));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void blankIrnIsForbidden() {
        UserAccount user = account(null, "Nameless");
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(user, "BUG", "Hello", null));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void sixFilesAreRejected() {
        UserAccount user = account("2331200001", "Nguyen Van A");
        MultipartFile[] files = new MultipartFile[6];
        for (int i = 0; i < 6; i++) {
            files[i] = new MockMultipartFile("files", "a" + i + ".java", "text/plain", "x".getBytes());
        }
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(user, "BUG", "Too many", files));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void oversizedUploadIsRejected() {
        UserAccount user = account("2331200001", "Nguyen Van A");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(FeedbackService.MAX_UPLOAD_BYTES + 1);
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(user, "BUG", "Big file", new MultipartFile[] { file }));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void fakeImageDoesNotPersist() {
        UserAccount user = account("2331200001", "Nguyen Van A");
        byte[] fakePng = new byte[] { 0, 1, 2 };
        MockMultipartFile png = new MockMultipartFile("files", "shot.png", "image/png", fakePng);
        when(imageConverter.convertRasterToWebp("shot.png", fakePng))
                .thenThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "not an image"));
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.create(user, "BUG", "Bad image", new MultipartFile[] { png }));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(itemRepository, never()).save(any());
    }

    private static UserAccount account(String irn, String name) {
        UserAccount user = new UserAccount();
        user.setStudentCode(irn);
        user.setFullName(name);
        user.setEmail("a@eiu.edu.vn");
        return user;
    }
}
