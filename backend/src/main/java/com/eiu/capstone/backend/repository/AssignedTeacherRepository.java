package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.AssignedTeacher;
import com.eiu.capstone.backend.model.AssignedTeacherId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignedTeacherRepository extends JpaRepository<AssignedTeacher, AssignedTeacherId> {
}