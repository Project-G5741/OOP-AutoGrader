package com.eiu.capstone.backend.service;

import com.eiu.capstone.backend.model.Course;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CourseService {

    private final List<Course> courses = new ArrayList<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public CourseService() {
        courses.add(new Course(idGenerator.getAndIncrement(), "CS101", "Introduction to Computer Science"));
        courses.add(new Course(idGenerator.getAndIncrement(), "MATH201", "Discrete Mathematics"));
    }

    public List<Course> getAllCourses() {
        return new ArrayList<>(courses);
    }

    public Optional<Course> getCourseById(Integer id) {
        return courses.stream()
                .filter(course -> course.getCourseId() != null && course.getCourseId().equals(id))
                .findFirst();
    }

    public Course createCourse(Course course) {
        Course savedCourse = new Course(
                idGenerator.getAndIncrement(),
                course.getCourseCode(),
                course.getCourseName()
        );
        courses.add(savedCourse);
        return savedCourse;
    }
}