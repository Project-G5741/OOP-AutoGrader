package com.eiu.capstone.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "labs", uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "lab_number"}))
public class Lab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lab_id")
    private Integer labId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(name = "lab_number", nullable = false)
    private Short labNumber;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    public Lab() {}

    public Integer getLabId() { return labId; }
    public void setLabId(Integer labId) { this.labId = labId; }

    public SchoolClass getSchoolClass() { return schoolClass; }
    public void setSchoolClass(SchoolClass schoolClass) { this.schoolClass = schoolClass; }

    public Short getLabNumber() { return labNumber; }
    public void setLabNumber(Short labNumber) { this.labNumber = labNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
}