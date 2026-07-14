package com.eiu.capstone.backend.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AssignedTeacherId implements Serializable {

    private Integer classId;
    private Integer teacherId;

    public AssignedTeacherId() {}

    public AssignedTeacherId(Integer classId, Integer teacherId) {
        this.classId = classId;
        this.teacherId = teacherId;
    }

    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignedTeacherId)) return false;
        AssignedTeacherId that = (AssignedTeacherId) o;
        return Objects.equals(classId, that.classId) && Objects.equals(teacherId, that.teacherId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classId, teacherId);
    }
}