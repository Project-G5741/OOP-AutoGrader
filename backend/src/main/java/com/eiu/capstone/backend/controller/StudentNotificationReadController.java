package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.StudentNotificationDismissBatchRequest;
import com.eiu.capstone.backend.DTO.StudentNotificationDismissRequest;
import com.eiu.capstone.backend.DTO.StudentNotificationMarkReadRequest;
import com.eiu.capstone.backend.DTO.StudentNotificationReadKeysDTO;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.StudentNotificationDismissService;
import com.eiu.capstone.backend.service.StudentNotificationReadService;

@RestController
@RequestMapping("/api/students/notification-reads")
@Profile("!desktop & !desktop-lecturer")
public class StudentNotificationReadController {

    private final JwtAuthHelper jwtAuthHelper;
    private final StudentNotificationReadService notificationReadService;
    private final StudentNotificationDismissService notificationDismissService;

    public StudentNotificationReadController(
            JwtAuthHelper jwtAuthHelper,
            StudentNotificationReadService notificationReadService,
            StudentNotificationDismissService notificationDismissService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.notificationReadService = notificationReadService;
        this.notificationDismissService = notificationDismissService;
    }

    @GetMapping("/list")
    public StudentNotificationReadKeysDTO list(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return notificationReadService.listKeys(user.getId());
    }

    @PostMapping("/mark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mark(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody(required = false) StudentNotificationMarkReadRequest body) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        notificationReadService.markRead(user.getId(), body == null ? null : body.key());
    }

    @GetMapping("/dismissed/list")
    public StudentNotificationReadKeysDTO listDismissed(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return notificationDismissService.listKeys(user.getId());
    }

    @PostMapping("/dismiss")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismiss(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody(required = false) StudentNotificationDismissRequest body) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        notificationDismissService.dismiss(user.getId(), body == null ? null : body.key());
    }

    @PostMapping("/dismiss/batch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dismissBatch(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody(required = false) StudentNotificationDismissBatchRequest body) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        notificationDismissService.dismissBatch(user.getId(), body == null ? null : body.keys());
    }
}
