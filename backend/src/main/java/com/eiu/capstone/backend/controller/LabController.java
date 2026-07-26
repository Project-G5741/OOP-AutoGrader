package com.eiu.capstone.backend.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.repository.LabRepository;

@RestController
@RequestMapping("/api/labs")
public class LabController {

    private final LabRepository labRepository;

    public LabController(LabRepository labRepository) {
        this.labRepository = labRepository;
    }

    @GetMapping
    public List<LabSummary> listLabs() {
        return labRepository.findAll().stream()
                .map(lab -> new LabSummary(lab.getId(), lab.getName()))
                .collect(Collectors.toList());
    }

    public record LabSummary(UUID id, String name) {}
}