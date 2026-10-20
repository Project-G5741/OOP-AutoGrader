package com.eiu.capstone.backend.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.feedback.FeedbackDetailDTO;
import com.eiu.capstone.backend.DTO.feedback.FeedbackListItemDTO;
import com.eiu.capstone.backend.DTO.feedback.FeedbackResponsePatch;
import com.eiu.capstone.backend.DTO.feedback.FeedbackStatusPatch;
import com.eiu.capstone.backend.model.FeedbackAttachment;
import com.eiu.capstone.backend.service.FeedbackService;

@RestController
@Profile("!desktop")
@RequestMapping("/api/lecturer/feedback")
public class LecturerFeedbackController {

    private final FeedbackService feedbackService;

    public LecturerFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/list")
    public List<FeedbackListItemDTO> list(@RequestParam(defaultValue = "OPEN") String status) {
        return feedbackService.list(status);
    }

    @GetMapping("/{id}")
    public FeedbackDetailDTO detail(@PathVariable UUID id) {
        return feedbackService.detail(id);
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> download(@PathVariable UUID id, @PathVariable UUID attachmentId) {
        FeedbackAttachment attachment = feedbackService.getAttachment(id, attachmentId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(attachment.getStoredContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        String filename = attachment.getOriginalFilename() == null ? "attachment" : attachment.getOriginalFilename();
        if ("image/webp".equals(attachment.getStoredContentType()) && !filename.toLowerCase().endsWith(".webp")) {
            int dot = filename.lastIndexOf('.');
            filename = (dot > 0 ? filename.substring(0, dot) : filename) + ".webp";
        }
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(attachment.getContent());
    }

    @PatchMapping("/{id}/status")
    public FeedbackDetailDTO updateStatus(@PathVariable UUID id, @RequestBody FeedbackStatusPatch body) {
        return feedbackService.updateStatus(id, body == null ? null : body.status());
    }

    @PatchMapping("/{id}/response")
    public FeedbackDetailDTO saveResponse(@PathVariable UUID id, @RequestBody FeedbackResponsePatch body) {
        return feedbackService.saveResponse(id, body == null ? null : body.response());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        feedbackService.deleteResolved(id);
    }
}
