package com.eiu.capstone.backend.DTO;

import java.util.List;

/**
 * Current-quarter enrolled student IRNs for Bulk Grading drop gates.
 * Includes inactive/suspended accounts; {@code student_code} only.
 */
public class BulkCurrentTermStudentIrnsResponse {

    private final List<String> irns;

    public BulkCurrentTermStudentIrnsResponse(List<String> irns) {
        this.irns = irns != null ? List.copyOf(irns) : List.of();
    }

    public List<String> getIrns() {
        return irns;
    }
}
