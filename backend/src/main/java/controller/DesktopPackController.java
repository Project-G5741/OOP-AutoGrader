package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.eiu.capstone.backend.desktop.DesktopPackImportService.LabNameConflict;
import com.eiu.capstone.backend.desktop.DesktopPackStageService;
import com.eiu.capstone.backend.desktop.DesktopPackStageService.LabNameConflictException;
import com.eiu.capstone.backend.desktop.DesktopPackStageService.StageResult;

@RestController
@RequestMapping("/api/desktop/packs")
@Profile("desktop")
public class DesktopPackController {

    public record ImportConflictDTO(java.util.List<LabNameConflict> conflicts, String message) {}

    public record ImportResultDTO(
            String kind,
            String filename,
            String message,
            boolean requiresRestart) {}

    private final DesktopPackStageService stageService;

    public DesktopPackController(DesktopPackStageService stageService) {
        this.stageService = stageService;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultDTO importPack(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmReplace", defaultValue = "false") boolean confirmReplace) {
        StageResult result = stageService.stageUpload(file, confirmReplace);
        return new ImportResultDTO(
                result.kind().name(),
                result.filename(),
                result.message(),
                true);
    }

    @ExceptionHandler(LabNameConflictException.class)
    public ResponseEntity<ImportConflictDTO> handleConflict(LabNameConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ImportConflictDTO(ex.conflicts(), ex.getMessage()));
    }
}
