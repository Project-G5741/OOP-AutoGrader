package com.eiu.capstone.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.MasterDataItemDTO;
import com.eiu.capstone.backend.repository.MasterDataRepository;

@RestController
@RequestMapping("/api/master-data")
public class MasterDataController {

    private final MasterDataRepository masterDataRepository;

    public MasterDataController(MasterDataRepository masterDataRepository) {
        this.masterDataRepository = masterDataRepository;
    }

    @GetMapping
    public List<MasterDataItemDTO> listByCategory(@RequestParam String category) {
        return masterDataRepository.findByCategoryOrderByNameAsc(category).stream()
                .map(row -> new MasterDataItemDTO(row.getId(), row.getName(), row.getCategory()))
                .toList();
    }
}
