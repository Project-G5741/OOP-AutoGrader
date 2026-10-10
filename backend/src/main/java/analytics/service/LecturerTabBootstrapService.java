package com.eiu.capstone.backend.analytics.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.eiu.capstone.backend.DTO.ChallengeDTO;
import com.eiu.capstone.backend.DTO.MasterDataItemDTO;
import com.eiu.capstone.backend.DTO.TermSummaryDTO;
import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.analytics.dto.AnalyticsDashboardResponse;
import com.eiu.capstone.backend.analytics.dto.GradeOverviewResponse;
import com.eiu.capstone.backend.analytics.dto.LecturerOverviewResponse;
import com.eiu.capstone.backend.analytics.dto.SolutionBootstrapResponse;
import com.eiu.capstone.backend.analytics.dto.UsersBootstrapResponse;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.MasterDataRepository;
import com.eiu.capstone.backend.service.ChallengeService;
import com.eiu.capstone.backend.service.LabDeadlineHelper;
import com.eiu.capstone.backend.service.LabStructureService;
import com.eiu.capstone.backend.service.TermService;
import com.eiu.capstone.backend.service.UserService;

@Service
public class LecturerTabBootstrapService {

    private static final int USERS_PAGE_SIZE = 50;
    private static final int SCORE_PAGE_SIZE = 12;

    private final LecturerAnalyticsService lecturerAnalyticsService;
    private final AnalyticsService analyticsService;
    private final TermService termService;
    private final UserService userService;
    private final LabRepository labRepository;
    private final LabDeadlineHelper labDeadlineHelper;
    private final MasterDataRepository masterDataRepository;
    private final LabStructureService labStructureService;
    private final ChallengeService challengeService;
    private final ExecutorService lecturerBootstrapExecutor;

    public LecturerTabBootstrapService(
            LecturerAnalyticsService lecturerAnalyticsService,
            AnalyticsService analyticsService,
            TermService termService,
            UserService userService,
            LabRepository labRepository,
            LabDeadlineHelper labDeadlineHelper,
            MasterDataRepository masterDataRepository,
            LabStructureService labStructureService,
            ChallengeService challengeService,
            @Qualifier("lecturerBootstrapExecutor") ExecutorService lecturerBootstrapExecutor) {
        this.lecturerAnalyticsService = lecturerAnalyticsService;
        this.analyticsService = analyticsService;
        this.termService = termService;
        this.userService = userService;
        this.labRepository = labRepository;
        this.labDeadlineHelper = labDeadlineHelper;
        this.masterDataRepository = masterDataRepository;
        this.labStructureService = labStructureService;
        this.challengeService = challengeService;
        this.lecturerBootstrapExecutor = lecturerBootstrapExecutor;
    }

    public LecturerOverviewResponse dashboard() {
        return lecturerAnalyticsService.getOverviewFresh();
    }

    public GradeOverviewResponse score() {
        return lecturerAnalyticsService.getGradeOverview(0, SCORE_PAGE_SIZE, "studentName,asc", null);
    }

    public List<SolutionBootstrapResponse.LabItem> gradingLabs() {
        List<Lab> labs = currentTermLabEntities();
        if (labs.isEmpty()) {
            return List.of();
        }
        List<UUID> labIds = labs.stream().map(Lab::getId).toList();
        Map<UUID, List<ChallengeDTO>> challengesByLab =
                challengeService.listSidebarChallengesByLabIds(labIds);
        return labs.stream()
                .map(lab -> toLabItem(lab, challengesByLab.getOrDefault(lab.getId(), List.of())))
                .toList();
    }

    public UsersBootstrapResponse users() {
        Page<UserAccount> page = userService.getAllUser(PageRequest.of(0, USERS_PAGE_SIZE));
        return new UsersBootstrapResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    public List<TermSummaryDTO> quarters() {
        return termService.listTerms();
    }

    public AnalyticsDashboardResponse reports() {
        return analyticsService.getDashboardFresh(null, null, null, null);
    }

    public SolutionBootstrapResponse solution() {
        ExecutorService pool = lecturerBootstrapExecutor;
        CompletableFuture<List<MasterDataItemDTO>> scopeF =
                CompletableFuture.supplyAsync(() -> masterDataItems("SCOPE"), pool);
        CompletableFuture<List<MasterDataItemDTO>> declaringF =
                CompletableFuture.supplyAsync(() -> masterDataItems("DECLARING_TYPE"), pool);
        CompletableFuture<List<MasterDataItemDTO>> relationF =
                CompletableFuture.supplyAsync(() -> masterDataItems("RELATION_TYPE"), pool);
        CompletableFuture<List<TermSummaryDTO>> termsF =
                CompletableFuture.supplyAsync(termService::listTerms, pool);
        CompletableFuture<List<SolutionBootstrapResponse.LabItem>> labsF =
                CompletableFuture.supplyAsync(this::currentTermLabs, pool);

        CompletableFuture<LabStructureResponse> structureF = labsF.thenComposeAsync(labs -> {
            if (labs.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            UUID labId = labs.get(0).id();
            return CompletableFuture.supplyAsync(
                    () -> labStructureService.loadForEditor(labId), pool);
        }, pool);

        try {
            CompletableFuture.allOf(scopeF, declaringF, relationF, termsF, labsF, structureF).join();
            List<SolutionBootstrapResponse.LabItem> labs = labsF.join();
            UUID selectedLabId = labs.isEmpty() ? null : labs.get(0).id();
            return new SolutionBootstrapResponse(
                    scopeF.join(),
                    declaringF.join(),
                    relationF.join(),
                    termsF.join(),
                    labs,
                    selectedLabId,
                    structureF.join());
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(cause);
        }
    }

    private List<MasterDataItemDTO> masterDataItems(String category) {
        return masterDataRepository.findByCategoryOrderByNameAsc(category).stream()
                .map(row -> new MasterDataItemDTO(row.getId(), row.getName(), row.getCategory()))
                .toList();
    }

    private List<SolutionBootstrapResponse.LabItem> currentTermLabs() {
        return currentTermLabEntities().stream()
                .map(lab -> toLabItem(lab, List.of()))
                .toList();
    }

    private List<Lab> currentTermLabEntities() {
        Term current = termService.findCurrentTerm().orElse(null);
        if (current == null) {
            return List.of();
        }
        return labRepository.findByTerm_Id(current.getId()).stream()
                .sorted(Comparator.comparing(Lab::getName, labDeadlineHelper.naturalLabNameComparator()))
                .toList();
    }

    private static SolutionBootstrapResponse.LabItem toLabItem(Lab lab, List<ChallengeDTO> challenges) {
        return new SolutionBootstrapResponse.LabItem(
                lab.getId(),
                lab.getName(),
                lab.getDeadlineDate(),
                lab.isStudentVisible(),
                challenges);
    }
}
