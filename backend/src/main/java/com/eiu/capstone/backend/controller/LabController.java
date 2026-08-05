package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.StatsDTO;
import com.eiu.capstone.backend.repository.LabRepository;
import com.eiu.capstone.backend.service.StatsService;

@RestController
@RequestMapping("/api/labs")
public class LabController {

    private final LabRepository labRepository;
    private final StatsService statsService;

    public LabController(LabRepository labRepository, StatsService statsService) {
        this.labRepository = labRepository;
        this.statsService = statsService;
    }

    @GetMapping
    public List<LabSummary> listLabs() {
        return labRepository.findAll().stream()
                .map(lab -> new LabSummary(lab.getId(), lab.getName()))
                .collect(Collectors.toList());
    }

    public record LabSummary(UUID id, String name) {}

    /** Lab-scoped stats for parallel dashboard load (same data as challenge stats route). */
    @GetMapping("/{labId}/stats")
    public StatsDTO getStats(@PathVariable UUID labId,
                             @RequestParam(required = false) UUID studentId) {
        return statsService.getStats(labId, studentId);
    }
}