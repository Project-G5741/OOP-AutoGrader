package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
}