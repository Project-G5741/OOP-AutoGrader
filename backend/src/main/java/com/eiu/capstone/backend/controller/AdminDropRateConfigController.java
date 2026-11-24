package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.eiu.capstone.backend.DTO.AdminDropRatesView;
import com.eiu.capstone.backend.DTO.EggSmashConfigView;
import com.eiu.capstone.backend.DTO.EggSmashRateUpdateRequest;
import com.eiu.capstone.backend.DTO.GachaConfigView;
import com.eiu.capstone.backend.DTO.GachaRateUpdateRequest;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtRoleNames;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.EggSmashRateConfigService;
import com.eiu.capstone.backend.service.GachaRateConfigService;

/**
 * Admin-only endpoints to inspect, update, and reset Gacha ({@code Crystal Wheel})
 * and {@code Egg Smash} item drop rates.
 */
@RestController
@RequestMapping("/api/lecturer/drop-rates")
@Profile("!desktop & !desktop-lecturer")
public class AdminDropRateConfigController {

    private final JwtAuthHelper jwtAuthHelper;
    private final GachaRateConfigService gachaRateConfigService;
    private final EggSmashRateConfigService eggSmashRateConfigService;

    public AdminDropRateConfigController(
            JwtAuthHelper jwtAuthHelper,
            GachaRateConfigService gachaRateConfigService,
            EggSmashRateConfigService eggSmashRateConfigService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.gachaRateConfigService = gachaRateConfigService;
        this.eggSmashRateConfigService = eggSmashRateConfigService;
    }

    @GetMapping("/config")
    public AdminDropRatesView getConfig(@AuthenticationPrincipal JwtUserPrincipal principal) {
        requireAdmin(principal);
        return new AdminDropRatesView(
                gachaRateConfigService.current().toView(),
                eggSmashRateConfigService.current().toView());
    }

    @PutMapping("/gacha")
    public GachaConfigView updateGachaRates(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody GachaRateUpdateRequest request) {
        requireAdmin(principal);
        return gachaRateConfigService.update(request).toView();
    }

    @PostMapping("/gacha/reset")
    public GachaConfigView resetGachaRates(@AuthenticationPrincipal JwtUserPrincipal principal) {
        requireAdmin(principal);
        return gachaRateConfigService.resetDefaults().toView();
    }

    @PutMapping("/egg-smash")
    public EggSmashConfigView updateEggSmashRates(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody EggSmashRateUpdateRequest request) {
        requireAdmin(principal);
        return eggSmashRateConfigService.update(request).toView();
    }

    @PostMapping("/egg-smash/reset")
    public EggSmashConfigView resetEggSmashRates(@AuthenticationPrincipal JwtUserPrincipal principal) {
        requireAdmin(principal);
        return eggSmashRateConfigService.resetDefaults().toView();
    }

    private void requireAdmin(JwtUserPrincipal principal) {
        jwtAuthHelper.requireActiveUser(principal);
        if (principal == null || !principal.hasRole(JwtRoleNames.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
        }
    }
}
