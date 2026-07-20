package com.eiu.capstone.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.eiu.capstone.backend.model.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @EntityGraph(attributePaths = "roles")
    @Query("select users from UserAccount")
    List<UserAccount> findAllWithRoles();

    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByStudentCode(String studentCode);
    Optional<UserAccount> findByTeacherCode(String teacherCode);
    Optional<UserAccount> findByStudentCodeOrTeacherCode(String studentCode, String teacherCode);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
