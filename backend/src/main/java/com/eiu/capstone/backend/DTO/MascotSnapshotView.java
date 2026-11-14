package com.eiu.capstone.backend.DTO;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MascotSnapshotView(String equipped, List<String> owned, String jti) {

    public MascotSnapshotView(String equipped, List<String> owned) {
        this(equipped, owned, null);
    }
}
