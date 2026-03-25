package com.gamematcher.controller.account;

import com.gamematcher.dto.account.SteamAccountLinkRequestDto;
import com.gamematcher.entity.account.SteamAccount;
import com.gamematcher.service.account.SteamAccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Steam 계정 연동 API
 */
@RestController
@RequestMapping("/api/steam")
public class SteamController {

    private final SteamAccountService steamAccountService;

    public SteamController(SteamAccountService steamAccountService) {
        this.steamAccountService = steamAccountService;
    }

    @PostMapping("/account/link")
    public SteamAccount linkAccount(@Valid @RequestBody SteamAccountLinkRequestDto request) {
        return steamAccountService.linkSteamAccount(
                request.getUserId(),
                request.getSteamId(),
                request.getPersonaName(),
                request.getAvatar()
        );
    }
}
