package unit.com.eiu.capstone.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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

import com.eiu.capstone.backend.DTO.feedback.FeedbackAuthorReplyDTO;
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
        when(itemRepository.findByStatusAndArchivedAtIsNullOrderByCreatedAtDesc("OPEN"))
                .thenReturn(List.of(lecturer, student));
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
        when(itemRepository.findByStatusAndArchivedAtIsNullOrderByCreatedAtDesc("OPEN")).thenReturn(List.of(open));
        when(itemRepository.findByStatusAndArchivedAtIsNullOrderByCreatedAtDesc("RESOLVED"))
                .thenReturn(List.of(resolved));
        when(itemRepository.findByArchivedAtIsNullOrderByCreatedAtDesc()).thenReturn(List.of(open, resolved));
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
        FeedbackItem resolved = item("RESOLVED", "1", "A", "done");
        FeedbackAttachment attachment = new FeedbackAttachment();
        attachment.setStoredContentType("image/webp");
        attachment.setOriginalFilename("shot.png");
        attachment.setContent(new byte[] { 1, 2 });
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(resolved));
        when(attachmentRepository.findByIdAndItem_Id(attachmentId, itemId)).thenReturn(Optional.of(attachment));

        FeedbackAttachment found = service.getAttachment(itemId, attachmentId);
        assertEquals("image/webp", found.getStoredContentType());
    }

    @Test
    void deleteResolvedWithReplySoftArchives() {
        UUID id = UUID.randomUUID();
        FeedbackItem resolved = item("RESOLVED", "1", "A", "done");
        resolved.setLecturerResponse("Fixed in lab 3");
        resolved.setResponseAt(OffsetDateTime.parse("2026-10-20T02:00:00Z"));
        when(itemRepository.findById(id)).thenReturn(Optional.of(resolved));
        when(itemRepository.save(resolved)).thenReturn(resolved);

        service.deleteResolved(id);

        assertNotNull(resolved.getArchivedAt());
        verify(itemRepository).save(resolved);
        verify(itemRepository, never()).delete(any());
    }

    @Test
    void deleteResolvedWithoutReplyHardDeletes() {
        UUID id = UUID.randomUUID();
        FeedbackItem resolved = item("RESOLVED", "1", "A", "done");
        when(itemRepository.findById(id)).thenReturn(Optional.of(resolved));

        service.deleteResolved(id);
        verify(itemRepository).delete(resolved);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void deleteResolvedWithWhitespaceOnlyReplyHardDeletes() {
        UUID id = UUID.randomUUID();
        FeedbackItem resolved = item("RESOLVED", "1", "A", "done");
        resolved.setLecturerResponse("   ");
        when(itemRepository.findById(id)).thenReturn(Optional.of(resolved));

        service.deleteResolved(id);
        verify(itemRepository).delete(resolved);
    }

    @Test
    void consumeRepliesHardDeletesOwnedRepliedItems() {
        UUID userId = UUID.randomUUID();
        UUID replyId = UUID.randomUUID();
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(userId);
        FeedbackItem archived = item("RESOLVED", "1", "A", "done");
        archived.setLecturerResponse("Fixed in lab 3");
        archived.setArchivedAt(OffsetDateTime.parse("2026-10-20T03:00:00Z"));
        when(itemRepository.findByIdInAndUser_IdAndLecturerResponseIsNotNull(List.of(replyId), userId))
                .thenReturn(List.of(archived));

        service.consumeRepliesForAuthor(user, List.of(replyId));

        verify(itemRepository).deleteAll(List.of(archived));
    }

    @Test
    void consumeRepliesWithEmptyIdsIsNoOp() {
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(UUID.randomUUID());

        service.consumeRepliesForAuthor(user, List.of());

        verify(itemRepository, never()).deleteAll(any());
        verify(itemRepository, never()).findByIdInAndUser_IdAndLecturerResponseIsNotNull(any(), any());
    }

    @Test
    void listRepliesForAuthorIncludesSoftArchived() {
        UUID userId = UUID.randomUUID();
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(userId);
        FeedbackItem archived = item("RESOLVED", "1", "A", "done");
        archived.setLecturerResponse("Fixed in lab 3");
        archived.setResponseAt(OffsetDateTime.parse("2026-10-20T02:00:00Z"));
        archived.setArchivedAt(OffsetDateTime.parse("2026-10-20T03:00:00Z"));
        when(itemRepository.findByUser_IdAndLecturerResponseIsNotNullOrderByResponseAtDesc(userId))
                .thenReturn(List.of(archived));

        List<FeedbackAuthorReplyDTO> replies = service.listRepliesForAuthor(user);
        assertEquals(1, replies.size());
        assertEquals("Fixed in lab 3", replies.get(0).lecturerResponse());
    }

    @Test
    void archivedItemIsNotFoundForDetail() {
        UUID id = UUID.randomUUID();
        FeedbackItem archived = item("RESOLVED", "1", "A", "done");
        archived.setArchivedAt(OffsetDateTime.parse("2026-10-20T03:00:00Z"));
        when(itemRepository.findById(id)).thenReturn(Optional.of(archived));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.detail(id));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
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
