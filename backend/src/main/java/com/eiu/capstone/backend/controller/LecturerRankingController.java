package com.eiu.capstone.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.analytics.dto.WeeklyRankingBoardResponse;
import com.eiu.capstone.backend.analytics.service.WeeklyRankingService;

@RestController
@RequestMapping("/api/lecturer/ranking")
public class LecturerRankingController {

    private final WeeklyRankingService weeklyRankingService;

    public LecturerRankingController(WeeklyRankingService weeklyRankingService) {
        this.weeklyRankingService = weeklyRankingService;
    }

    @GetMapping("/current")
    public WeeklyRankingBoardResponse current() {
        return weeklyRankingService.currentLiveBoard();
    }
}
