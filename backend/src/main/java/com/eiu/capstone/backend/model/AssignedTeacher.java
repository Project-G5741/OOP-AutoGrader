package com.eiu.capstone.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "assigned_teachers")
public class AssignedTeacher {

    @EmbeddedId
    private AssignedTeacherId id = new AssignedTeacherId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("classId")
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teacherId")
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = true;

    public AssignedTeacher() {}

    public AssignedTeacherId getId() { return id; }
    public void setId(AssignedTeacherId id) { this.id = id; }

    public SchoolClass getSchoolClass() { return schoolClass; }
    public void setSchoolClass(SchoolClass schoolClass) { this.schoolClass = schoolClass; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
}