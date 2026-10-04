package com.eiu.capstone.backend.desktop.pack;

import java.time.LocalDate;
import java.util.UUID;

public record DesktopPackLabMeta(
        UUID id,
        String name,
        boolean studentVisible,
        LocalDate releaseDate,
        LocalDate deadlineDate) {}
