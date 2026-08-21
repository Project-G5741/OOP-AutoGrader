package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TermRepository extends JpaRepository<Term, UUID> {
    List<Term> findByAcademicYear_Id(UUID academicYearId);
    Optional<Term> findByAcademicYear_IdAndTermNumber(UUID academicYearId, int termNumber);

    @Query("SELECT t FROM Term t JOIN FETCH t.academicYear")
    List<Term> findAllWithAcademicYear();

    @Query("SELECT t FROM Term t WHERE t.current = true")
    Optional<Term> findCurrent();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Term t SET t.current = false WHERE t.current = true AND t.id <> :termId")
    int clearOtherCurrent(@Param("termId") UUID termId);
}