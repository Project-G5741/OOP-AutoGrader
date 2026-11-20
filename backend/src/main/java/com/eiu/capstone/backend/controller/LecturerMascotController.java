package com.eiu.capstone.backend.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.LecturerMascotOwnershipRow;
import com.eiu.capstone.backend.service.LecturerMascotOwnershipService;

@RestController
@RequestMapping("/api/lecturer/mascots")
@Profile("!desktop & !desktop-lecturer")
public class LecturerMascotController {

    private final LecturerMascotOwnershipService ownershipService;

    public LecturerMascotController(LecturerMascotOwnershipService ownershipService) {
        this.ownershipService = ownershipService;
    }

    @GetMapping("/ownership")
    public List<LecturerMascotOwnershipRow> ownership() {
        return ownershipService.listCurrentTermStudents();
    }
}