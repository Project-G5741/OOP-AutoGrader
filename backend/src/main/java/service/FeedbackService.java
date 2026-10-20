package com.eiu.capstone.backend.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.feedback.ConvertedAttachment;
import com.eiu.capstone.backend.DTO.feedback.FeedbackAttachmentMetaDTO;
import com.eiu.capstone.backend.DTO.feedback.FeedbackAuthorReplyDTO;
import com.eiu.capstone.backend.DTO.feedback.FeedbackCreatedDTO;
import com.eiu.capstone.backend.DTO.feedback.FeedbackDetailDTO;
import com.eiu.capstone.backend.DTO.feedback.FeedbackListItemDTO;
import com.eiu.capstone.backend.model.FeedbackAttachment;
import com.eiu.capstone.backend.model.FeedbackItem;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.FeedbackAttachmentRepository;
import com.eiu.capstone.backend.repository.FeedbackItemRepository;

@Service
public class FeedbackService {

    public static final int MAX_FILES = 5;
    public static final long MAX_UPLOAD_BYTES = 25L * 1024 * 1024;
    public static final int MAX_MESSAGE_CHARS = 4000;
    public static final int MAX_RESPONSE_CHARS = 500;
    public static final int PREVIEW_CHARS = 160;

    static final String TYPE_BUG = "BUG";
    static final String TYPE_CONTRIBUTION = "CONTRIBUTION";
    static final String STATUS_OPEN = "OPEN";
    static final String STATUS_RESOLVED = "RESOLVED";
    static final String FILTER_ALL = "ALL";

    private static final Set<String> ALLOWED_TYPES = Set.of(TYPE_BUG, TYPE_CONTRIBUTION);
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_OPEN, STATUS_RESOLVED);
    private static final Set<String> IMAGE_SUFFIXES = Set.of("png", "jpg", "jpeg");

    private final FeedbackItemRepository itemRepository;
    private final FeedbackAttachmentRepository attachmentRepository;
    private final FeedbackImageConverter imageConverter;

    public FeedbackService(
            FeedbackItemRepository itemRepository,
            FeedbackAttachmentRepository attachmentRepository,
            FeedbackImageConverter imageConverter) {
        this.itemRepository = itemRepository;
        this.attachmentRepository = attachmentRepository;
        this.imageConverter = imageConverter;
    }

    @Transactional
    public FeedbackCreatedDTO create(UserAccount user, String type, String message, MultipartFile[] files) {
        String irn = user.getIrn();
        if (irn == null || irn.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is missing an IRN.");
        }
        String itemType = normalizeType(type);
        String trimmed = message == null ? "" : message.trim();
        if (trimmed.isEmpty()) {
            throw unprocessable("Message is required.");
        }
        if (trimmed.length() > MAX_MESSAGE_CHARS) {
            throw unprocessable("Message must be at most 4000 characters.");
        }

        List<MultipartFile> incoming = presentFiles(files);
        if (incoming.size() > MAX_FILES) {
            throw unprocessable("You can attach at most 5 files.");
        }
        long totalBytes = 0;
        for (MultipartFile file : incoming) {
            totalBytes += file.getSize();
            if (totalBytes > MAX_UPLOAD_BYTES) {
                throw unprocessable("Attachments must be at most 25 MB for one upload.");
            }
        }

        List<ConvertedAttachment> converted = new ArrayList<>();
        for (MultipartFile file : incoming) {
            converted.add(convertFile(file));
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        FeedbackItem item = new FeedbackItem();
        item.setUser(user);
        item.setAuthorIrn(irn);
        item.setAuthorName(user.getFullName());
        item.setItemType(itemType);
        item.setMessage(trimmed);
        item.setStatus(STATUS_OPEN);
        item.setCreatedAt(now);

        for (ConvertedAttachment part : converted) {
            FeedbackAttachment attachment = new FeedbackAttachment();
            attachment.setItem(item);
            attachment.setOriginalFilename(part.originalFilename());
            attachment.setStoredContentType(part.storedContentType());
            attachment.setContent(part.content());
            attachment.setByteSize(part.content().length);
            item.getAttachments().add(attachment);
        }

        FeedbackItem saved = itemRepository.save(item);
        return new FeedbackCreatedDTO(saved.getId(), saved.getItemType(), saved.getStatus(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<FeedbackListItemDTO> list(String statusFilter) {
        String filter = statusFilter == null || statusFilter.isBlank()
                ? STATUS_OPEN
                : statusFilter.trim().toUpperCase(Locale.ROOT);
        List<FeedbackItem> items;
        if (FILTER_ALL.equals(filter)) {
            items = itemRepository.findAllByOrderByCreatedAtDesc();
        } else if (ALLOWED_STATUSES.contains(filter)) {
            items = itemRepository.findByStatusOrderByCreatedAtDesc(filter);
        } else {
            throw unprocessable("Status filter must be OPEN, RESOLVED, or ALL.");
        }
        List<FeedbackListItemDTO> rows = new ArrayList<>();
        for (FeedbackItem item : items) {
            int count = (int) attachmentRepository.countByItem_Id(item.getId());
            rows.add(new FeedbackListItemDTO(
                    item.getId(),
                    item.getItemType(),
                    preview(item.getMessage()),
                    item.getAuthorIrn(),
                    item.getAuthorName(),
                    item.getStatus(),
                    item.getCreatedAt(),
                    count));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public FeedbackDetailDTO detail(UUID id) {
        FeedbackItem item = requireItem(id);
        List<FeedbackAttachmentMetaDTO> attachments = attachmentRepository.findMetaByItemId(id).stream()
                .map(meta -> new FeedbackAttachmentMetaDTO(
                        meta.getId(),
                        meta.getOriginalFilename(),
                        meta.getStoredContentType(),
                        meta.getByteSize()))
                .toList();
        return new FeedbackDetailDTO(
                item.getId(),
                item.getItemType(),
                item.getMessage(),
                item.getAuthorIrn(),
                item.getAuthorName(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getResolvedAt(),
                item.getLecturerResponse(),
                item.getResponseAt(),
                attachments);
    }

    @Transactional(readOnly = true)
    public List<FeedbackAuthorReplyDTO> listRepliesForAuthor(UserAccount user) {
        if (user.getId() == null) {
            return List.of();
        }
        return itemRepository.findByUser_IdAndLecturerResponseIsNotNullOrderByResponseAtDesc(user.getId())
                .stream()
                .map(item -> new FeedbackAuthorReplyDTO(
                        item.getId(),
                        item.getItemType(),
                        item.getLecturerResponse(),
                        item.getResponseAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public FeedbackAttachment getAttachment(UUID itemId, UUID attachmentId) {
        return attachmentRepository.findByIdAndItem_Id(attachmentId, itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
    }

    @Transactional
    public FeedbackDetailDTO updateStatus(UUID id, String status) {
        String next = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(next)) {
            throw unprocessable("Status must be OPEN or RESOLVED.");
        }
        FeedbackItem item = requireItem(id);
        item.setStatus(next);
        if (STATUS_RESOLVED.equals(next)) {
            item.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
        } else {
            item.setResolvedAt(null);
            item.setLecturerResponse(null);
            item.setResponseAt(null);
        }
        itemRepository.save(item);
        return detail(id);
    }

    @Transactional
    public FeedbackDetailDTO saveResponse(UUID id, String response) {
        FeedbackItem item = requireItem(id);
        if (!STATUS_RESOLVED.equals(item.getStatus())) {
            throw unprocessable("Reply is only available on resolved feedback.");
        }
        String trimmed = response == null ? "" : response.trim();
        if (trimmed.isEmpty()) {
            throw unprocessable("A short response is required.");
        }
        if (trimmed.length() > MAX_RESPONSE_CHARS) {
            throw unprocessable("Response must be at most 500 characters.");
        }
        item.setLecturerResponse(trimmed);
        item.setResponseAt(OffsetDateTime.now(ZoneOffset.UTC));
        itemRepository.save(item);
        return detail(id);
    }

    @Transactional
    public void deleteResolved(UUID id) {
        FeedbackItem item = requireItem(id);
        if (!STATUS_RESOLVED.equals(item.getStatus())) {
            throw unprocessable("Only resolved feedback can be deleted.");
        }
        itemRepository.delete(item);
    }

    private FeedbackItem requireItem(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback item not found"));
    }

    private ConvertedAttachment convertFile(MultipartFile file) {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String suffix = FeedbackImageConverter.suffixOf(original);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw unprocessable("Could not read an attached file.");
        }
        if (IMAGE_SUFFIXES.contains(suffix)) {
            return imageConverter.convertRasterToWebp(original, bytes);
        }
        if ("java".equals(suffix)) {
            return imageConverter.passthroughSource(original, "text/x-java-source", bytes);
        }
        if ("mmd".equals(suffix)) {
            return imageConverter.passthroughSource(original, "text/plain", bytes);
        }
        throw unprocessable("Only .png, .jpg, .java, and .mmd files are allowed.");
    }

    private static List<MultipartFile> presentFiles(MultipartFile[] files) {
        List<MultipartFile> present = new ArrayList<>();
        if (files == null) {
            return present;
        }
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                present.add(file);
            }
        }
        return present;
    }

    private static String normalizeType(String type) {
        String normalized = type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw unprocessable("Choose Bug report or Contribution.");
        }
        return normalized;
    }

    private static String preview(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() <= PREVIEW_CHARS) {
            return message;
        }
        return message.substring(0, PREVIEW_CHARS);
    }

    private static ResponseStatusException unprocessable(String message) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
}
