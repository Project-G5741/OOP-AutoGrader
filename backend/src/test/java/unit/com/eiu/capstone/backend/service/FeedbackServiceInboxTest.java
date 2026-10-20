package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.feedback.FeedbackListItemDTO;
import com.eiu.capstone.backend.model.FeedbackAttachment;
import com.eiu.capstone.backend.model.FeedbackItem;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.FeedbackAttachmentRepository;
import com.eiu.capstone.backend.repository.FeedbackItemRepository;
import com.eiu.capstone.backend.service.FeedbackImageConverter;
import com.eiu.capstone.backend.service.FeedbackService;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceInboxTest {

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
    }

    @Test
    void defaultListIsOpenNewestAndShowsEveryAuthor() {
        FeedbackItem student = item("OPEN", "2331200001", "Student A", "Need help");
        FeedbackItem lecturer = item("OPEN", "T001", "Lecturer B", "Idea");
        when(itemRepository.findByStatusOrderByCreatedAtDesc("OPEN")).thenReturn(List.of(lecturer, student));
        when(attachmentRepository.countByItem_Id(any())).thenReturn(0L);

        List<FeedbackListItemDTO> rows = service.list(null);
        assertEquals(2, rows.size());
        assertEquals("T001", rows.get(0).authorIrn());
        assertEquals("2331200001", rows.get(1).authorIrn());
        assertEquals("Lecturer B", rows.get(0).authorName());
        assertEquals("Student A", rows.get(1).authorName());
    }

    @Test
    void resolvedIsHiddenUntilFilterChanges() {
        FeedbackItem open = item("OPEN", "1", "A", "open");
        FeedbackItem resolved = item("RESOLVED", "2", "B", "done");
        when(itemRepository.findByStatusOrderByCreatedAtDesc("OPEN")).thenReturn(List.of(open));
        when(itemRepository.findByStatusOrderByCreatedAtDesc("RESOLVED")).thenReturn(List.of(resolved));
        when(itemRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(open, resolved));
        when(attachmentRepository.countByItem_Id(any())).thenReturn(0L);

        assertEquals(1, service.list("OPEN").size());
        assertEquals("OPEN", service.list("OPEN").get(0).status());
        assertEquals(1, service.list("RESOLVED").size());
        assertEquals(2, service.list("ALL").size());
    }

    @Test
    void resolveThenReopen() {
        UUID id = UUID.randomUUID();
        FeedbackItem item = item("OPEN", "1", "A", "msg");
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        when(attachmentRepository.findMetaByItemId(id)).thenReturn(List.of());

        service.updateStatus(id, "RESOLVED");
        assertEquals("RESOLVED", item.getStatus());

        service.updateStatus(id, "OPEN");
        assertEquals("OPEN", item.getStatus());
        assertNull(item.getResolvedAt());
        assertNull(item.getLecturerResponse());
    }

    @Test
    void saveResponseOnResolved() {
        UUID id = UUID.randomUUID();
        FeedbackItem item = item("RESOLVED", "1", "A", "msg");
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);
        when(attachmentRepository.findMetaByItemId(id)).thenReturn(List.of());

        service.saveResponse(id, "  Fixed in the next lab.  ");
        assertEquals("Fixed in the next lab.", item.getLecturerResponse());
    }

    @Test
    void saveResponseOnOpenIsRejected() {
        UUID id = UUID.randomUUID();
        FeedbackItem item = item("OPEN", "1", "A", "msg");
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> service.saveResponse(id, "Thanks"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void downloadReturnsStoredWebpType() {
        UUID itemId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        FeedbackAttachment attachment = new FeedbackAttachment();
        attachment.setStoredContentType("image/webp");
        attachment.setOriginalFilename("shot.png");
        attachment.setContent(new byte[] { 1, 2 });
        when(attachmentRepository.findByIdAndItem_Id(attachmentId, itemId)).thenReturn(Optional.of(attachment));

        FeedbackAttachment found = service.getAttachment(itemId, attachmentId);
        assertEquals("image/webp", found.getStoredContentType());
    }

    @Test
    void deleteResolvedRemovesItem() {
        UUID id = UUID.randomUUID();
        FeedbackItem resolved = item("RESOLVED", "1", "A", "done");
        when(itemRepository.findById(id)).thenReturn(Optional.of(resolved));

        service.deleteResolved(id);
        verify(itemRepository).delete(resolved);
    }

    @Test
    void deleteOpenIsRejected() {
        UUID id = UUID.randomUUID();
        FeedbackItem open = item("OPEN", "1", "A", "open");
        when(itemRepository.findById(id)).thenReturn(Optional.of(open));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.deleteResolved(id));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        verify(itemRepository, never()).delete(any());
    }

    @Test
    void unknownIdIsNotFound() {
        UUID id = UUID.randomUUID();
        when(itemRepository.findById(id)).thenReturn(Optional.empty());
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.detail(id));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private static FeedbackItem item(String status, String irn, String name, String message) {
        FeedbackItem item = new FeedbackItem();
        item.setUser(new UserAccount());
        item.setAuthorIrn(irn);
        item.setAuthorName(name);
        item.setItemType("BUG");
        item.setMessage(message);
        item.setStatus(status);
        item.setCreatedAt(OffsetDateTime.parse("2026-10-20T01:00:00Z"));
        return item;
    }
}
