package com.eiu.capstone.backend.controller;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.desktop.DesktopBulkBatchStore;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/desktop/bulk-batch")
@Profile("desktop-lecturer")
public class DesktopBulkBatchController {

    private final DesktopBulkBatchStore bulkBatchStore;

    public DesktopBulkBatchController(DesktopBulkBatchStore bulkBatchStore) {
        this.bulkBatchStore = bulkBatchStore;
    }

    @GetMapping
    public ResponseEntity<?> load() {
        JsonNode batch = bulkBatchStore.load();
        if (batch == null) {
            return ResponseEntity.ok(Map.of("present", false));
        }
        return ResponseEntity.ok(Map.of("present", true, "batch", batch));
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody JsonNode body) {
        bulkBatchStore.save(body);
        return ResponseEntity.ok(Map.of("saved", true));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> clear() {
        bulkBatchStore.clear();
        return ResponseEntity.ok(Map.of("cleared", true));
    }
}
