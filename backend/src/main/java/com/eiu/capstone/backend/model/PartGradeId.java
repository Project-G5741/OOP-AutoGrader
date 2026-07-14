package com.eiu.capstone.backend.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PartGradeId implements Serializable {

    private Integer submissionId;
    private Integer partId;

    public PartGradeId() {}

    public PartGradeId(Integer submissionId, Integer partId) {
        this.submissionId = submissionId;
        this.partId = partId;
    }

    public Integer getSubmissionId() { return submissionId; }
    public void setSubmissionId(Integer submissionId) { this.submissionId = submissionId; }

    public Integer getPartId() { return partId; }
    public void setPartId(Integer partId) { this.partId = partId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PartGradeId)) return false;
        PartGradeId that = (PartGradeId) o;
        return Objects.equals(submissionId, that.submissionId) && Objects.equals(partId, that.partId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(submissionId, partId);
    }
}