package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.desktop.DesktopRosterImportService;

@RestController
@RequestMapping("/api/desktop/roster")
@Profile("desktop-lecturer")
public class DesktopRosterController {

    private final DesktopRosterImportService rosterImportService;

    public DesktopRosterController(DesktopRosterImportService rosterImportService) {
        this.rosterImportService = rosterImportService;
    }

    public record RosterImportRequest(List<String> studentCodes) {}

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        boolean ready = rosterImportService.hasUsableRoster();
        return ResponseEntity.ok(Map.of(
                "ready", ready,
                "blocked", !ready));
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importRoster(@RequestBody RosterImportRequest body) {
        DesktopRosterImportService.ImportResult result =
                rosterImportService.replaceImport(body != null ? body.studentCodes() : List.of());
        return ResponseEntity.ok(Map.of(
                "importedCount", result.importedCount(),
                "blocked", result.blocked(),
                "ready", !result.blocked()));
    }
}
