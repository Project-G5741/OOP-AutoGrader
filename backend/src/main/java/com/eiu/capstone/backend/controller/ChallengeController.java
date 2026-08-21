package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.ChallengeDTO;
import com.eiu.capstone.backend.DTO.ClassTabResponse;
import com.eiu.capstone.backend.DTO.MmdResponseDTO;
import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.DTO.TestcaseResultDTO;
import com.eiu.capstone.backend.analytics.dto.ChallengeStudentRowDTO;
import com.eiu.capstone.backend.analytics.service.LecturerAnalyticsService;
import com.eiu.capstone.backend.service.ChallengeService;
import com.eiu.capstone.backend.service.ClassStructureService;
import com.eiu.capstone.backend.service.StatsService;

@RestController
@RequestMapping("/api/labs/{labId}/challenges")
@CrossOrigin // adjust/remove to match your existing CORS config
public class ChallengeController {

    private final ChallengeService challengeService;
    private final ClassStructureService classStructureService;
    private final StatsService statsService;
    private final LecturerAnalyticsService lecturerAnalyticsService;

    public ChallengeController(ChallengeService challengeService,
                                ClassStructureService classStructureService,
                                StatsService statsService,
                                LecturerAnalyticsService lecturerAnalyticsService) {
        this.challengeService = challengeService;
        this.classStructureService = classStructureService;
        this.statsService = statsService;
        this.lecturerAnalyticsService = lecturerAnalyticsService;
    }

    /** Powers the Challenges sidebar. studentId is optional so the list still loads (with score=null) if omitted. */
    @GetMapping
    public List<ChallengeDTO> getChallenges(@PathVariable UUID labId,
                                             @RequestParam(required = false) UUID studentId) {
        return challengeService.getChallengesForLab(labId, studentId);
    }

    /** Powers the "MMD" tab. Returns {@code { classes, parseError }} when the student has a submission.
     *  submissionId pins the response to a specific submission (e.g. the one just graded);
     *  when omitted, falls back to the student's latest submission for this lab. */
    @GetMapping("/{challengeId}/mmd")
    public MmdResponseDTO getMmd(@PathVariable UUID labId,
                                 @PathVariable UUID challengeId,
                                 @RequestParam(required = false) UUID studentId,
                                 @RequestParam(required = false) UUID submissionId) {
        return classStructureService.getMmdData(labId, challengeId, studentId, submissionId);
    }

    /** Powers the "Class" tab. Returns [] when the student has no reference submission yet.
     *  submissionId pins the response to a specific submission (e.g. the one just graded);
     *  when omitted, falls back to the student's latest submission for this lab. */
    @GetMapping("/{challengeId}/class")
    public ClassTabResponse getClassData(@PathVariable UUID labId,
                                              @PathVariable UUID challengeId,
                                              @RequestParam(required = false) UUID studentId,
                                              @RequestParam(required = false) UUID submissionId) {
        return classStructureService.getClassData(labId, challengeId, studentId, submissionId);
    }

    /** Powers the "Operation Test" tab. Returns [] when the student has no reference submission yet.
     *  submissionId pins the response to a specific submission (e.g. the one just graded);
     *  when omitted, falls back to the student's latest submission for this lab. */
    @GetMapping("/{challengeId}/testcases")
    public List<TestcaseResultDTO> getTestcases(@PathVariable UUID labId,
                                                @PathVariable UUID challengeId,
                                                @RequestParam(required = false) UUID studentId,
                                                @RequestParam(required = false) UUID submissionId) {
        return classStructureService.getTestcaseData(labId, challengeId, studentId, submissionId);
    }

    /**
     * Powers the 3 stat cards (Current Grade / Total Submissions / Latest
     * Submission). challengeId is accepted for route symmetry with the
     * frontend's existing fetch call but isn't used — stats are tracked per
     * (student, lab) via student_lab_progress, not per challenge.
     */
    @GetMapping("/{challengeId}/stats")
    public StatsDTO getStats(@PathVariable UUID labId,
                              @PathVariable UUID challengeId,
                              @RequestParam(required = false) UUID studentId) {
        return statsService.getStats(labId, studentId);
    }

    /** Paginated enrolled-student roster for a challenge tab (one row per student). */
    @GetMapping("/{challengeId}/students")
    public Page<ChallengeStudentRowDTO> getChallengeStudents(@PathVariable UUID labId,
                                                           @PathVariable UUID challengeId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "5") int size,
                                                           @RequestParam(required = false) String sort) {
        return lecturerAnalyticsService.getChallengeStudentRoster(labId, challengeId, page, size, sort);
    }
}

