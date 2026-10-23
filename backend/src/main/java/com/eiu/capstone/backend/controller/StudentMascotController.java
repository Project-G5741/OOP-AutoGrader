package com.eiu.capstone.backend.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eiu.capstone.backend.DTO.MascotShopView;
import com.eiu.capstone.backend.model.UserAccount;
import com.eiu.capstone.backend.security.JwtAuthHelper;
import com.eiu.capstone.backend.security.JwtUserPrincipal;
import com.eiu.capstone.backend.service.StudentMascotService;

@RestController
@RequestMapping("/api/students/mascots")
public class StudentMascotController {

    private final JwtAuthHelper jwtAuthHelper;
    private final StudentMascotService studentMascotService;

    public StudentMascotController(JwtAuthHelper jwtAuthHelper, StudentMascotService studentMascotService) {
        this.jwtAuthHelper = jwtAuthHelper;
        this.studentMascotService = studentMascotService;
    }

    @GetMapping("/list")
    public MascotShopView list(@AuthenticationPrincipal JwtUserPrincipal principal) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentMascotService.list(user.getId());
    }

    @PostMapping("/{mascotId}/purchase")
    public MascotShopView purchase(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String mascotId) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentMascotService.purchase(user.getId(), mascotId);
    }

    @PostMapping("/{mascotId}/equip")
    public MascotShopView equip(
            @AuthenticationPrincipal JwtUserPrincipal principal,
            @PathVariable String mascotId) {
        UserAccount user = jwtAuthHelper.requireActiveUser(principal);
        return studentMascotService.equip(user.getId(), mascotId);
    }
}
