package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Term;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Integer> {
}