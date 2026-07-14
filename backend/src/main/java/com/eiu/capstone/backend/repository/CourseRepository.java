package com.eiu.capstone.backend.repository;

import com.eiu.capstone.backend.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Integer> {
}