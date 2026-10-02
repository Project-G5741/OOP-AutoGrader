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

import com.eiu.capstone.backend.grading.rubric.LabRubricSnapshot;
import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.repository.TermRepository;
import com.eiu.capstone.backend.grading.rubric.LabRubricService;
import com.eiu.capstone.backend.service.TermService;

@Service
public class DesktopPackExportService {

    private final TermRepository termRepository;
    private final LabRepository labRepository;
    private final LabRubricService labRubricService;
    private final DesktopPackSerializer serializer;
    private final DesktopPackCrypto crypto;
    private final DesktopPackSigningKeys signingKeys;

    public DesktopPackExportService(
            TermRepository termRepository,
            LabRepository labRepository,
            LabRubricService labRubricService,
            DesktopPackSerializer serializer,
            DesktopPackSigningKeys signingKeys) {
        this.termRepository = termRepository;
        this.labRepository = labRepository;
        this.labRubricService = labRubricService;
        this.serializer = serializer;
        this.signingKeys = signingKeys;
        this.crypto = DesktopPackCrypto.withDefaultEmbeddedKey();
    }

    @Transactional(readOnly = true)
    public byte[] exportLabPack(UUID labId) {
        PrivateKey privateKey = signingKeys.signingPrivateKey()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Desktop practice pack export is not configured on this server"));
        Lab lab = labRepository.findByIdWithTerm(labId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab not found"));
        Term term = lab.getTerm();
        if (term == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lab is not assigned to a quarter");
        }
        Instant createdAt = Instant.now();
        String termLabel = TermService.buildTermLabel(term);
        String packVersion = lab.getId() + "-" + createdAt.toEpochMilli();
        DesktopPackLabEntry entry = toEntry(lab);
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
        try {
            DesktopPackFile pack = crypto.buildPack(privateKey, manifest, innerJson);
            return crypto.serializeFile(pack);
        } catch (GeneralSecurityException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build desktop pack");
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportTermPack(UUID termId) {
        PrivateKey privateKey = signingKeys.signingPrivateKey()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Desktop practice pack export is not configured on this server"));
        Term term = termRepository.findByIdWithAcademicYear(termId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quarter not found"));
        List<Lab> labs = labRepository.findByTerm_Id(termId).stream()
                .sorted(Comparator.comparing(Lab::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (labs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quarter has no labs to export");
        }
        Instant createdAt = Instant.now();
        String termLabel = TermService.buildTermLabel(term);
        String packVersion = term.getId() + "-" + createdAt.toEpochMilli();
        List<DesktopPackLabEntry> entries = labs.stream()
                .map(lab -> toEntry(lab))
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
        try {
            DesktopPackFile pack = crypto.buildPack(privateKey, manifest, innerJson);
            return crypto.serializeFile(pack);
        } catch (GeneralSecurityException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to build desktop pack");
        }
    }

    private DesktopPackLabEntry toEntry(Lab lab) {
        LabRubricSnapshot rubric = labRubricService.loadForLab(lab);
        DesktopPackLabMeta meta = new DesktopPackLabMeta(
                lab.getId(),
                lab.getName(),
                lab.isStudentVisible(),
                lab.getReleaseDate(),
                lab.getDeadlineDate());
        return new DesktopPackLabEntry(meta, rubric);
    }
}
