package com.eiu.capstone.backend.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.DTO.feedback.FeedbackAuthorReplyDTO;
import com.eiu.capstone.backend.DTO.feedback.FeedbackConsumeRepliesRequest;
import com.eiu.capstone.backend.DTO.feedback.FeedbackCreatedDTO;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.FeedbackService;

@RestController
@Profile("!desktop")
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final JwtAuthHelper jwtAuthHelper;

    public FeedbackController(FeedbackService feedbackService, JwtAuthHelper jwtAuthHelper) {
        this.feedbackService = feedbackService;
        this.jwtAuthHelper = jwtAuthHelper;
    }

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackCreatedDTO create(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestParam("type") String type,
            @RequestParam("message") String message,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return feedbackService.create(user, type, message, files);
    }

    @GetMapping("/replies")
    public List<FeedbackAuthorReplyDTO> replies(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return feedbackService.listRepliesForAuthor(user);
    }

    @DeleteMapping("/replies")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void consumeReplies(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody(required = false) FeedbackConsumeRepliesRequest body) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        feedbackService.consumeRepliesForAuthor(user, body == null ? List.of() : body.ids());
    }
}
