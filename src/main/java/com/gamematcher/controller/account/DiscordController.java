package com.gamematcher.controller.account;

import com.gamematcher.dto.account.DiscordAccountLinkRequestDto;
import com.gamematcher.entity.account.DiscordAccount;
import com.gamematcher.service.account.DiscordAccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Discord 계정 연동 API
 */
@RestController
@RequestMapping("/api/discord")
public class DiscordController {

    private final DiscordAccountService discordAccountService;

    public DiscordController(DiscordAccountService discordAccountService) {
        this.discordAccountService = discordAccountService;
    }

    @PostMapping("/account/link")
    public DiscordAccount linkAccount(@Valid @RequestBody DiscordAccountLinkRequestDto request) {
        return discordAccountService.linkDiscordAccount(
                request.getUserId(),
                request.getDiscordId(),
                request.getUsername(),
                request.getAvatar()
        );
    }
}
