package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.model.MasterData;
import com.eiu.capstone.backend.repository.MasterDataRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MasterDataResolver {

    private final MasterDataRepository masterDataRepository;

    public MasterDataResolver(MasterDataRepository masterDataRepository) {
        this.masterDataRepository = masterDataRepository;
    }

    public Map<Integer, String> loadAll() {
        return masterDataRepository.findAll().stream()
                .collect(Collectors.toMap(MasterData::getId, MasterData::getName));
    }
}