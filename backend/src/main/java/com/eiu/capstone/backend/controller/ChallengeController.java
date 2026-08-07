package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;

import com.eiu.capstone.backend.DTO.ChallengeDTO;
import com.eiu.capstone.backend.DTO.ClassDetailDTO;
import com.eiu.capstone.backend.DTO.MmdClassDTO;
import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.service.ChallengeService;
import com.eiu.capstone.backend.service.ClassStructureService;
import com.eiu.capstone.backend.service.StatsService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/labs/{labId}/challenges")
@CrossOrigin // adjust/remove to match your existing CORS config
public class ChallengeController {

    private final ChallengeService challengeService;
    private final ClassStructureService classStructureService;
    private final StatsService statsService;

    public ChallengeController(ChallengeService challengeService,
                                ClassStructureService classStructureService,
                                StatsService statsService) {
        this.challengeService = challengeService;
        this.classStructureService = classStructureService;
        this.statsService = statsService;
    }

    /** Powers the Challenges sidebar. studentId is optional so the list still loads (with score=null) if omitted. */
    @GetMapping
    public List<ChallengeDTO> getChallenges(@PathVariable UUID labId,
                                             @RequestParam(required = false) UUID studentId) {
        return challengeService.getChallengesForLab(labId, studentId);
    }

    /** Powers the "MMD" tab. Returns [] when the student has no reference submission yet.
     *  submissionId pins the response to a specific submission (e.g. the one just graded);
     *  when omitted, falls back to the student's latest submission for this lab. */
    @GetMapping("/{challengeId}/mmd")
    public List<MmdClassDTO> getMmd(@PathVariable UUID labId,
                                     @PathVariable UUID challengeId,
                                     @RequestParam(required = false) UUID studentId,
                                     @RequestParam(required = false) UUID submissionId) {
        return classStructureService.getMmdData(labId, challengeId, studentId, submissionId);
    }

    /** Powers the "Class" tab. Returns [] when the student has no reference submission yet.
     *  submissionId pins the response to a specific submission (e.g. the one just graded);
     *  when omitted, falls back to the student's latest submission for this lab. */
    @GetMapping("/{challengeId}/class")
    public List<ClassDetailDTO> getClassData(@PathVariable UUID labId,
                                              @PathVariable UUID challengeId,
                                              @RequestParam(required = false) UUID studentId,
                                              @RequestParam(required = false) UUID submissionId) {
        return classStructureService.getClassData(labId, challengeId, studentId, submissionId);
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

    // NOTE: no /{challengeId}/testcases endpoint yet — there's no table in
    // DbContext.docx backing test cases (input/expectedOutput/hidden flag),
    // so it's skipped per your instruction until that model exists.
}
