package com.eiu.capstone.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.GachaSpendRequest;
import com.eiu.capstone.backend.DTO.StudentStreakView;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.StudentStreakService;

@RestController
@RequestMapping("/api/students/streak")
@Profile("!desktop")
public class StudentStreakController {

    private final JwtAuthHelper jwtAuthHelper;
    private final StudentStreakService studentStreakService;

    public StudentStreakController(JwtAuthHelper jwtAuthHelper, StudentStreakService studentStreakService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.studentStreakService = studentStreakService;
    }

    @GetMapping
    public StudentStreakView current(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentStreakService.visit(user.getId());
    }

    @PostMapping("/pet")
    public StudentStreakView pet(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentStreakService.pet(user.getId());
    }

    @PostMapping("/restore")
    public StudentStreakView restore(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentStreakService.restore(user.getId());
    }

    @PostMapping("/gacha-spend")
    public StudentStreakView spendGacha(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody GachaSpendRequest request) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentStreakService.spendGachaTickets(user.getId(), request.count());
    }

    @PostMapping("/crystals/grant")
    public StudentStreakView grantCrystals(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody GachaSpendRequest request) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentStreakService.grantCrystals(user.getId(), request.count());
    }

    @PostMapping("/crystals/spend")
    public StudentStreakView spendCrystals(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody GachaSpendRequest request) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentStreakService.spendCrystals(user.getId(), request.count());
    }

    @PostMapping("/crystals/exchange")
    public StudentStreakView exchangeCrystals(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @RequestBody GachaSpendRequest request) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentStreakService.exchangeCrystals(user.getId(), request.count());
    }
}
