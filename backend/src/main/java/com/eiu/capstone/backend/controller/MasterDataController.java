package com.eiu.capstone.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.MasterDataItemDTO;
import com.eiu.capstone.backend.repository.MasterDataRepository;
import com.eiu.capstone.backend.security.JwtAuthHelper;

@RestController
@RequestMapping("/api/master-data")
public class MasterDataController {

    private final MasterDataRepository masterDataRepository;
    private final JwtAuthHelper jwtAuthHelper;

    public MasterDataController(MasterDataRepository masterDataRepository, JwtAuthHelper jwtAuthHelper) {
        this.masterDataRepository = masterDataRepository;
        this.jwtAuthHelper = jwtAuthHelper;
    }

    @GetMapping
    public List<MasterDataItemDTO> listByCategory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam String category) {
        jwtAuthHelper.requireLecturer(authHeader);
        return masterDataRepository.findByCategoryOrderByNameAsc(category).stream()
                .map(row -> new MasterDataItemDTO(row.getId(), row.getName(), row.getCategory()))
                .toList();
    }
}
