package com.eiu.capstone.backend.desktop.pack;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.grading.rubric.LabRubricCache;
import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.service.TermService;

@Service
public class DesktopPackExportService {

    public record DesktopPackDownload(byte[] body, String filename) {}

    private final TermRepository termRepository;
    private final LabRepository labRepository;
    private final LabRubricCache labRubricCache;
    private final DesktopPackSerializer serializer;
    private final DesktopPackCrypto crypto;
    private final DesktopPackSigningKeys signingKeys;

    public DesktopPackExportService(
            TermRepository termRepository,
            LabRepository labRepository,
            LabRubricCache labRubricCache,
            DesktopPackSerializer serializer,
            DesktopPackSigningKeys signingKeys) {
        this.termRepository = termRepository;
        this.labRepository = labRepository;
        this.labRubricCache = labRubricCache;
        this.serializer = serializer;
        this.signingKeys = signingKeys;
        this.crypto = DesktopPackCrypto.withDefaultEmbeddedKey();
    }

    @Transactional(readOnly = true)
    public DesktopPackDownload exportLabPack(UUID labId) {
        PrivateKey privateKey = requireSigningKey();
        Lab lab = labRepository.findByIdWithTerm(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        Term term = lab.getTerm();
        if (term == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lab is not assigned to a quarter");
        }
        String labName = LabNameRules.requireValid(lab.getName());
        Instant createdAt = Instant.now();
        String termLabel = TermService.buildTermLabel(term);
        String packVersion = lab.getId() + "-" + createdAt.toEpochMilli();
        DesktopPackLabEntry entry = toEntry(lab, labName);
        DesktopPackInnerPayload inner =
                new DesktopPackInnerPayload(packVersion, term.getId(), termLabel, List.of(entry));
        byte[] innerJson = serializer.toJson(inner);
        DesktopPackManifest manifest = new DesktopPackManifest(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                packVersion,
                term.getId(),
                termLabel,
                createdAt,
                List.of(lab.getId()),
                "placeholder");
        return new DesktopPackDownload(serialize(privateKey, manifest, innerJson), DesktopPackFileNames.labFilename(labName));
    }

    @Transactional(readOnly = true)
    public DesktopPackDownload exportTermPack(UUID termId) {
        PrivateKey privateKey = requireSigningKey();
        Term term = termRepository.findByIdWithAcademicYear(termId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quarter not found"));
        List<Lab> labs = labRepository.findByTerm_Id(termId).stream()
                .sorted(Comparator.comparing(Lab::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (labs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quarter has no labs to export");
        }
        for (Lab lab : labs) {
            LabNameRules.requireValid(lab.getName());
        }
        String yearLabel = term.getAcademicYear() != null ? term.getAcademicYear().getYearLabel() : "";
        Instant createdAt = Instant.now();
        String termLabel = TermService.buildTermLabel(term);
        String packVersion = term.getId() + "-" + createdAt.toEpochMilli();
        List<DesktopPackLabEntry> entries = labs.stream()
                .map(lab -> toEntry(lab, LabNameRules.requireValid(lab.getName())))
                .toList();
        DesktopPackInnerPayload inner = new DesktopPackInnerPayload(packVersion, term.getId(), termLabel, entries);
        byte[] innerJson = serializer.toJson(inner);
        List<UUID> labIds = labs.stream().map(Lab::getId).toList();
        DesktopPackManifest manifest = new DesktopPackManifest(
                DesktopPackManifest.CURRENT_FORMAT_VERSION,
                packVersion,
                term.getId(),
                termLabel,
                createdAt,
                labIds,
                "placeholder");
        String filename = DesktopPackFileNames.termFilename(yearLabel, term.getTermNumber());
        return new DesktopPackDownload(serialize(privateKey, manifest, innerJson), filename);
    }

    private PrivateKey requireSigningKey() {
        return signingKeys.signingPrivateKey()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Desktop practice pack export is not configured on this server"));
    }

    private byte[] serialize(PrivateKey privateKey, DesktopPackManifest manifest, byte[] innerJson) {
        try {
            DesktopPackFile pack = crypto.buildPack(privateKey, manifest, innerJson);
            return crypto.serializeFile(pack);
        } catch (GeneralSecurityException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build desktop pack");
        }
    }

    private DesktopPackLabEntry toEntry(Lab lab, String validatedName) {
        LabRubricSnapshot rubric = labRubricCache.get(lab);
        DesktopPackLabMeta meta = new DesktopPackLabMeta(
                lab.getId(),
                validatedName,
                lab.isStudentVisible(),
                lab.getReleaseDate(),
                lab.getDeadlineDate());
        return new DesktopPackLabEntry(meta, rubric);
    }
}
