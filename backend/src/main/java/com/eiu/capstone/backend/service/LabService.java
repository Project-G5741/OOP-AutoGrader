package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.model.Lab;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.repository.LabRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LabService {

    private final LabRepository labRepository;

    public LabService(LabRepository labRepository) {
        this.labRepository = labRepository;
    }

    public Lab createLab(String name, Term term) {
        Lab lab = new Lab();
        lab.setName(name);
        lab.setTerm(term);
        return labRepository.save(lab);
    }

    public Optional<Lab> getLab(UUID labId) {
        return labRepository.findById(labId);
    }

    public List<Lab> getLabsForTerm(UUID termId) {
        return labRepository.findByTerm_Id(termId);
    }

    public void deleteLab(UUID labId) {
        labRepository.deleteById(labId);
    }
}