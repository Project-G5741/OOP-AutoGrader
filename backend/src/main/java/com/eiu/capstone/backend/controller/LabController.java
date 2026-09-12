package com.eiu.capstone.backend.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.ChallengeDTO;
import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.analytics.dto.LabAttemptHistoryItemDTO;
import com.eiu.capstone.backend.analytics.dto.LabStatisticsResponse;
import com.eiu.capstone.backend.analytics.dto.SubmissionSummaryDTO;
import com.eiu.capstone.backend.analytics.service.LecturerAnalyticsService;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.ChallengeService;
import com.eiu.capstone.backend.service.LabDeadlineHelper;
import com.eiu.capstone.backend.service.LabDeadlineHelper.UrgencyState;
import com.eiu.capstone.backend.service.StatsService;
import com.eiu.capstone.backend.service.StudentTermAccessService;
import com.eiu.capstone.backend.service.TermService;

@RestController
@RequestMapping("/api/labs")
public class LabController {

    private final LabRepository labRepository;
    private final StatsService statsService;
    private final ChallengeService challengeService;
    private final LecturerAnalyticsService lecturerAnalyticsService;
    private final LabDeadlineHelper labDeadlineHelper;
    private final JwtAuthHelper jwtAuthHelper;
    private final TermService termService;
    private final StudentTermAccessService studentTermAccessService;

    public LabController(LabRepository labRepository,
                         StatsService statsService,
                         ChallengeService challengeService,
                         LecturerAnalyticsService lecturerAnalyticsService,
                         LabDeadlineHelper labDeadlineHelper,
                         JwtAuthHelper jwtAuthHelper,
                         TermService termService,
                         StudentTermAccessService studentTermAccessService) {
        this.labRepository = labRepository;
        this.statsService = statsService;
        this.challengeService = challengeService;
        this.lecturerAnalyticsService = lecturerAnalyticsService;
        this.labDeadlineHelper = labDeadlineHelper;
        this.jwtAuthHelper = jwtAuthHelper;
        this.termService = termService;
        this.studentTermAccessService = studentTermAccessService;
    }

    @GetMapping
    public List<LabSummary> listLabs(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        List<Lab> labs = labsVisibleToCaller(principal, user).stream()
                .sorted(Comparator.comparing(Lab::getName, labDeadlineHelper.naturalLabNameComparator()))
                .toList();
        if (labs.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<ChallengeDTO>> challengesByLab = Map.of();
        Map<UUID, StatsDTO> statsByLab = Map.of();
        if (principal.hasRole(JwtRoleNames.STUDENT)) {
            List<UUID> labIds = labs.stream().map(Lab::getId).toList();
            challengesByLab = challengeService.listSidebarChallengesByLabIds(labIds);
            statsByLab = statsService.getStatsForLabs(user.getId(), labIds);
        }
        Map<UUID, List<ChallengeDTO>> challenges = challengesByLab;
        Map<UUID, StatsDTO> stats = statsByLab;
        return labs.stream()
                .map(lab -> toSummary(
                        lab,
                        challenges.getOrDefault(lab.getId(), List.of()),
                        stats.get(lab.getId())))
                .toList();
    }

    private List<Lab> labsVisibleToCaller(JwtUserPrincipal principal, UserAccount user) {
        Term current = termService.findCurrentTerm().orElse(null);
        if (current == null) {
            return List.of();
        }
        if (!principal.isStudentOnly()) {
            return labRepository.findByTerm_Id(current.getId());
        }
        if (!termService.isEnrolled(user.getId(), current.getId())) {
            return List.of();
        }
        Instant now = Instant.now();
        List<Lab> visible = labRepository.findByTerm_Id(current.getId()).stream()
                .filter(lab -> labDeadlineHelper.isOpenForStudentSubmission(
                        lab.isStudentVisible(), lab.getReleaseDate(), now))
                .toList();
        for (Lab lab : visible) {
            if (lab.getTerm() == null) {
                lab.setTerm(current);
            }
            studentTermAccessService.rememberSuccessfulAccess(user, lab);
        }
        return visible;
    }

    private LabSummary toSummary(Lab lab, List<ChallengeDTO> challenges, StatsDTO stats) {
        UrgencyState urgency = labDeadlineHelper.urgencyState(lab.getDeadlineDate(), Instant.now());
        return new LabSummary(
                lab.getId(),
                lab.getName(),
                lab.getDeadlineDate(),
                urgency.name(),
                lab.isStudentVisible(),
                lab.getReleaseDate(),
                challenges,
                stats == null ? null : stats.totalSubmissions(),
                stats == null ? null : stats.latestSubmission());
    }

    public record LabSummary(
            UUID id,
            String name,
            LocalDate deadlineDate,
            String urgencyState,
            boolean studentVisible,
            LocalDate releaseDate,
            List<ChallengeDTO> challenges,
            Integer totalSubmissions,
            String latestSubmission) {}

    /** Lab-scoped stats for parallel dashboard load (same data as challenge stats route). */
    @GetMapping("/{labId}/stats")
    public StatsDTO getStats(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable UUID labId,
            @RequestParam(required = false) UUID studentId) {
        UUID scopedStudentId = jwtAuthHelper.resolveStudentScope(principal, studentId);
        if (principal != null && principal.isStudentOnly()) {
            studentTermAccessService.requireUploadAccess(principal.email(), labId);
        }
        return statsService.getStats(labId, scopedStudentId);
    }

    @GetMapping("/{labId}/statistics")
    public LabStatisticsResponse getStatistics(@PathVariable UUID labId) {
        return lecturerAnalyticsService.getLabStatistics(labId);
    }

    @GetMapping("/{labId}/submissions")
    public org.springframework.data.domain.Page<SubmissionSummaryDTO> getSubmissions(
            @PathVariable UUID labId,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "5") int size,
                                                     @RequestParam(required = false) String sort,
                                                     @RequestParam(required = false) String afterName,
                                                     @RequestParam(required = false) UUID afterId,
                                                     @RequestParam(required = false) String search) {
        return lecturerAnalyticsService.getLabSubmissions(labId, page, size, sort, afterName, afterId, search);
    }

    @GetMapping("/{labId}/submissions/export")
    public List<SubmissionSummaryDTO> exportSubmissions(
            @PathVariable UUID labId,
                                                        @RequestParam(required = false) String sort) {
        return lecturerAnalyticsService.getLabSubmissionsExport(labId, sort);
    }

    @GetMapping("/{labId}/students/{studentId}/attempts")
    public List<LabAttemptHistoryItemDTO> getStudentAttempts(
            @PathVariable UUID labId,
                                                             @PathVariable UUID studentId) {
        return lecturerAnalyticsService.getLabAttemptHistory(labId, studentId);
    }
}
