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

import com.eiu.capstone.backend.DTO.StudentNotificationMarkReadRequest;
import com.eiu.capstone.backend.DTO.StudentNotificationReadKeysDTO;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.StudentNotificationReadService;

@RestController
@RequestMapping("/api/students/notification-reads")
@Profile("!desktop & !desktop-lecturer")
public class StudentNotificationReadController {

    private final JwtAuthHelper jwtAuthHelper;
    private final StudentNotificationReadService notificationReadService;

    public StudentNotificationReadController(
            JwtAuthHelper jwtAuthHelper,
            StudentNotificationReadService notificationReadService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.notificationReadService = notificationReadService;
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
}
