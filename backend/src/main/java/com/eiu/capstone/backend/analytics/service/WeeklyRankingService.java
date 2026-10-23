package com.eiu.capstone.backend.analytics.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.analytics.dto.WeeklyRankingBoardResponse;
import com.eiu.capstone.backend.analytics.dto.WeeklyRankingRow;
import com.eiu.capstone.backend.analytics.repository.WeeklyRankingRepository;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.service.TermService;
import com.eiu.capstone.backend.service.WeeklyRankingCalendar;
import com.eiu.capstone.backend.service.WeeklyRankingRules;

@Service
public class WeeklyRankingService {

    private final TermService termService;
    private final WeeklyRankingRepository weeklyRankingRepository;

    public WeeklyRankingService(TermService termService, WeeklyRankingRepository weeklyRankingRepository) {
        this.termService = termService;
        this.weeklyRankingRepository = weeklyRankingRepository;
    }

    public WeeklyRankingBoardResponse currentLiveBoard() {
        Instant now = Instant.now();
        return boardAsOf(now, now);
    }

    public WeeklyRankingBoardResponse boardAsOf(Instant clock, Instant asOf) {
        Instant weekStart = WeeklyRankingCalendar.weekStartOf(clock);
        Instant weekEnd = WeeklyRankingCalendar.weekEndOf(clock);
        Optional<Term> current = termService.findCurrentTerm();
        if (current.isEmpty()) {
            return new WeeklyRankingBoardResponse(weekStart, weekEnd, List.of());
        }
        List<WeeklyRankingRepository.RankSource> sources =
                weeklyRankingRepository.findLiveBoardSources(current.get().getId(), asOf);
        return new WeeklyRankingBoardResponse(weekStart, weekEnd, toRows(sources));
    }

    public static List<WeeklyRankingRow> toRows(List<WeeklyRankingRepository.RankSource> sources) {
        List<WeeklyRankingRow> rows = new ArrayList<>(sources.size());
        int rank = 1;
        for (WeeklyRankingRepository.RankSource source : sources) {
            rows.add(new WeeklyRankingRow(
                    rank,
                    source.userId(),
                    source.fullName(),
                    source.irn(),
                    source.total(),
                    WeeklyRankingRules.giftPointsForRank(rank)));
            rank++;
        }
        return rows;
    }
}
