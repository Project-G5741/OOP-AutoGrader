package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.desktop.bundle.DesktopBundlePrefetchService;
import com.eiu.capstone.backend.desktop.bundle.DesktopBundlePrefetchService.DesktopBundleRefreshReport;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.security.JwtUserPrincipal;

/**
 * Re-pull offline install zips from {@code PRACTICE_BUNDLE_URL} / {@code LECTURER_BULK_BUNDLE_URL}
 * after GitHub stable-release assets change (no image rebuild).
 */
@RestController
@RequestMapping("/api/lecturer")
@Profile("!desktop & !desktop-lecturer")
public class LecturerDesktopBundleRefreshController {

    private final JwtAuthHelper jwtAuthHelper;
    private final DesktopBundlePrefetchService prefetchService;

    public LecturerDesktopBundleRefreshController(
            JwtAuthHelper jwtAuthHelper,
            DesktopBundlePrefetchService prefetchService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.prefetchService = prefetchService;
    }

    @PostMapping("/desktop-bundles/refresh")
    public DesktopBundleRefreshReport refreshDesktopBundles(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        jwtAuthHelper.requireActiveUser(principal);
        if (!principal.hasRole(JwtRoleNames.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
        return prefetchService.refreshAll();
    }
}