package com.eiu.capstone.backend.DTO;

import java.util.List;

public record TermRosterDTO(List<TermStudentDTO> enrolled, List<TermStudentDTO> available) {
}
