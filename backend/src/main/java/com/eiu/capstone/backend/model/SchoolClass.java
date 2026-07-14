package com.eiu.capstone.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "classes", uniqueConstraints = @UniqueConstraint(columnNames = {"term_id", "course_id"}))
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_id")
    private Integer classId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    public SchoolClass() {}

    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public Term getTerm() { return term; }
    public void setTerm(Term term) { this.term = term; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
}