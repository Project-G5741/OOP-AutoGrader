package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.eiu.capstone.backend.model.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @EntityGraph(attributePaths = "roles")
    @Query("select users from UserAccount users")
    List<UserAccount> findAllWithRoles();

    @EntityGraph(attributePaths = "roles")
    @Query("select users from UserAccount users")
    Page<UserAccount> findAllWithRoles(Pageable pageable);

    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByStudentCode(String studentCode);
    Optional<UserAccount> findByTeacherCode(String teacherCode);

    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByStudentCodeOrTeacherCode(String studentCode, String teacherCode);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
