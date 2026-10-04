package com.eiu.capstone.backend.desktop;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public final class DesktopLocalIdentity {

    public static final UUID USER_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    public static final String EMAIL = "practice@desktop.local";
    public static final String IRN = "DESKTOP";
    public static final String FULL_NAME = "Practice Student";

    private DesktopLocalIdentity() {}
}
