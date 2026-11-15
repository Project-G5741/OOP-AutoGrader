package com.eiu.capstone.backend.desktop;

import java.util.UUID;

/**
 * Synthetic local lecturer principal for the {@code desktop-lecturer} profile.
 * Distinct from student practice {@link DesktopLocalIdentity}.
 */
public final class DesktopLecturerLocalIdentity {

    public static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");
    public static final String EMAIL = "lecturer@desktop.local";
    public static final String IRN = "LECTURER";
    public static final String FULL_NAME = "Offline Lecturer";

    private DesktopLecturerLocalIdentity() {}
}
