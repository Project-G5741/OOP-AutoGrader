package com.eiu.capstone.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.analytics.dto.WeeklyRankingBoardResponse;
import com.eiu.capstone.backend.analytics.service.WeeklyRankingService;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.StudentTermAccessService;
import com.eiu.capstone.backend.service.WeeklyRankingCalendar;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/students/ranking")
public class StudentRankingController {

    private final JwtAuthHelper jwtAuthHelper;
    private final StudentTermAccessService studentTermAccessService;
    private final WeeklyRankingService weeklyRankingService;

    public StudentRankingController(
            JwtAuthHelper jwtAuthHelper,
            StudentTermAccessService studentTermAccessService,
            WeeklyRankingService weeklyRankingService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.studentTermAccessService = studentTermAccessService;
        this.weeklyRankingService = weeklyRankingService;
    }

    @GetMapping("/current")
    public WeeklyRankingBoardResponse current(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        if (!studentTermAccessService.isInCurrentTerm(user)) {
            Instant now = Instant.now();
            return new WeeklyRankingBoardResponse(
                    WeeklyRankingCalendar.weekStartOf(now),
                    WeeklyRankingCalendar.weekEndOf(now),
                    List.of());
        }
        return weeklyRankingService.currentLiveBoard();
    }
}
