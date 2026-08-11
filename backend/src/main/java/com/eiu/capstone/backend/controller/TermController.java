package com.eiu.capstone.backend.controller;

import java.util.Comparator;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.TermSummaryDTO;
import com.eiu.capstone.backend.model.Term;
import com.eiu.capstone.backend.repository.TermRepository;

@RestController
@RequestMapping("/api/terms")
public class TermController {

    private final TermRepository termRepository;

    public TermController(TermRepository termRepository) {
        this.termRepository = termRepository;
    }

    @GetMapping
    public List<TermSummaryDTO> listTerms() {
        return termRepository.findAllWithAcademicYear().stream()
                .sorted(Comparator
                        .comparing((Term t) -> t.getAcademicYear().getYearLabel()).reversed()
                        .thenComparing(Term::getTermNumber))
                .map(term -> new TermSummaryDTO(
                        term.getId(),
                        term.getAcademicYear().getYearLabel() + " — Term " + term.getTermNumber()))
                .toList();
    }
}
