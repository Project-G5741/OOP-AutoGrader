package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.MascotSnapshotView;
import com.eiu.capstone.backend.desktop.DesktopMascotSnapshotService;

@RestController
@RequestMapping("/api/desktop/mascot")
@Profile("desktop")
public class DesktopMascotSyncController {

    public record SyncRequest(String token) {
    }

    private final DesktopMascotSnapshotService snapshots;

    public DesktopMascotSyncController(DesktopMascotSnapshotService snapshots) {
        this.snapshots = snapshots;
    }

    @GetMapping("/snapshot")
    public MascotSnapshotView snapshot() {
        return snapshots.read();
    }

    @PostMapping("/sync")
    public MascotSnapshotView sync(@RequestBody SyncRequest body) {
        return snapshots.sync(body == null ? null : body.token());
    }
}
