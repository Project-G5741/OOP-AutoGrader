package com.eiu.capstone.backend.desktop;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eiu.capstone.backend.DTO.rubric.LabStructureResponse;
import com.eiu.capstone.backend.DTO.rubric.testcase.TestcaseStructureDTO;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabMeta;
import com.eiu.capstone.backend.desktop.pack.DesktopPackInnerPayload;
import com.eiu.capstone.backend.desktop.pack.DesktopPackLabEntry;
import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.model.AcademicYear;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.repository.AcademicYearRepository;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.TermEnrollmentRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.service.LabStructureService;
import com.eiu.capstone.backend.service.TestcaseRubricService;

@Service
@Profile("desktop")
public class DesktopPackImportService {

    private static final Pattern QUARTER_PATTERN =
            Pattern.compile("Quarter\\s+(\\d+)", Pattern.CASE_INSENSITIVE);

    private final LabRepository labRepository;
    private final TermRepository termRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermEnrollmentRepository termEnrollmentRepository;
    private final LabStructureService labStructureService;
    private final TestcaseRubricService testcaseRubricService;
    private final DesktopPackStructureMapper structureMapper;
    private final DesktopLocalUserService localUserService;
    private final LabRubricCache labRubricCache;

    public DesktopPackImportService(
            LabRepository labRepository,
            TermRepository termRepository,
            AcademicYearRepository academicYearRepository,
            TermEnrollmentRepository termEnrollmentRepository,
            LabStructureService labStructureService,
            TestcaseRubricService testcaseRubricService,
            DesktopPackStructureMapper structureMapper,
            DesktopLocalUserService localUserService,
            LabRubricCache labRubricCache) {
        this.labRepository = labRepository;
        this.termRepository = termRepository;
        this.academicYearRepository = academicYearRepository;
        this.termEnrollmentRepository = termEnrollmentRepository;
        this.labStructureService = labStructureService;
        this.testcaseRubricService = testcaseRubricService;
        this.structureMapper = structureMapper;
        this.localUserService = localUserService;
        this.labRubricCache = labRubricCache;
    }

    @Transactional
    public void importTermPack(DesktopPackInnerPayload payload) {
        wipePracticeData();
        Term term = ensureTerm(payload);
        localUserService.ensureLocalStudent(term);
        for (DesktopPackLabEntry entry : payload.labs()) {
            importLab(entry, term.getId());
        }
        labRubricCache.invalidateAll();
    }

    /**
     * Adds or replaces one lab from a lab pack without wiping other labs.
     */
    @Transactional
    public void importLabPack(DesktopPackInnerPayload payload) {
        if (payload.labs().size() != 1) {
            throw new IllegalArgumentException("Lab pack must contain exactly one lab");
        }
        DesktopPackLabEntry entry = payload.labs().get(0);
        Term term = ensureTerm(payload);
        localUserService.ensureLocalStudent(term);
        UUID labId = entry.lab().id();
        String labName = entry.lab().name();
        if (labRepository.existsById(labId)) {
            labStructureService.deleteLabsCascadeBulk(List.of(labId));
        }
        for (Lab existing : labRepository.findByTerm_IdAndNameIgnoreCase(term.getId(), labName)) {
            if (!existing.getId().equals(labId)) {
                labStructureService.deleteLabsCascadeBulk(List.of(existing.getId()));
            }
        }
        importLab(entry, term.getId());
        labRubricCache.invalidateAll();
    }

    public List<LabNameConflict> findLabNameConflicts(DesktopPackInnerPayload payload) {
        if (payload.labs().size() != 1) {
            return List.of();
        }
        DesktopPackLabEntry entry = payload.labs().get(0);
        UUID incomingId = entry.lab().id();
        String name = entry.lab().name();
        UUID termId = payload.termId();
        return labRepository.findByTerm_Id(termId).stream()
                .filter(lab -> lab.getName().equalsIgnoreCase(name) && !lab.getId().equals(incomingId))
                .map(lab -> new LabNameConflict(lab.getId(), lab.getName()))
                .toList();
    }

    public record LabNameConflict(UUID existingLabId, String labName) {}

    /** @deprecated use {@link #importTermPack} */
    @Transactional
    public void importPack(DesktopPackInnerPayload payload) {
        importTermPack(payload);
    }

    private void wipePracticeData() {
        List<UUID> labIds = labRepository.findAll().stream().map(Lab::getId).toList();
        if (!labIds.isEmpty()) {
            labStructureService.deleteLabsCascadeBulk(labIds);
        }
        termEnrollmentRepository.deleteAllInBatch();
        termRepository.deleteAllInBatch();
    }

    private Term ensureTerm(DesktopPackInnerPayload payload) {
        String yearLabel = parseYearLabel(payload.termLabel());
        int termNumber = parseTermNumber(payload.termLabel());
        AcademicYear year = academicYearRepository.findByYearLabel(yearLabel)
                .orElseGet(() -> {
                    AcademicYear created = new AcademicYear();
                    created.setYearLabel(yearLabel);
                    return academicYearRepository.save(created);
                });
        termRepository.clearOtherCurrent(payload.termId());
        Term term = termRepository.findById(payload.termId()).orElseGet(Term::new);
        if (term.getId() == null) {
            term.setId(payload.termId());
        }
        term.setAcademicYear(year);
        term.setTermNumber(termNumber);
        term.setCurrent(true);
        return termRepository.save(term);
    }

    private void importLab(DesktopPackLabEntry entry, UUID termId) {
        DesktopPackLabMeta meta = entry.lab();
        Lab lab = new Lab();
        lab.setId(meta.id());
        lab.setName(meta.name());
        lab.setTerm(termRepository.getReferenceById(termId));
        lab.setStudentVisible(meta.studentVisible());
        lab.setReleaseDate(meta.releaseDate());
        lab.setDeadlineDate(meta.deadlineDate());
        labRepository.save(lab);

        LabStructureResponse structure = structureMapper.toStructure(meta, entry.rubric(), termId);
        labStructureService.saveLabStructureInsertOnly(meta.id(), structure);
        Map<UUID, List<TestcaseStructureDTO>> otByChallenge =
                structureMapper.testcasesByChallenge(entry.rubric());
        if (!otByChallenge.isEmpty()) {
            testcaseRubricService.persistClonedTestcasesBatch(meta.id(), otByChallenge);
        }
        labRubricCache.invalidate(meta.id());
    }

    private static String parseYearLabel(String termLabel) {
        if (termLabel == null || termLabel.isBlank()) {
            return "Desktop";
        }
        int dash = termLabel.indexOf('—');
        if (dash < 0) {
            dash = termLabel.indexOf('-');
        }
        if (dash > 0) {
            return termLabel.substring(0, dash).trim();
        }
        return termLabel.trim();
    }

    private static int parseTermNumber(String termLabel) {
        if (termLabel == null) {
            return 1;
        }
        Matcher matcher = QUARTER_PATTERN.matcher(termLabel);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }
}
