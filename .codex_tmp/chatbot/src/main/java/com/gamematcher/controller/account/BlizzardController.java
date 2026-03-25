package com.gamematcher.controller.account;

import com.gamematcher.dto.account.BlizzardAccountLinkRequestDto;
import com.gamematcher.entity.account.BlizzardAccount;
import com.gamematcher.service.account.BlizzardAccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Blizzard Battle.net 계정 연동 API
 */
@RestController
@RequestMapping("/api/blizzard")
public class BlizzardController {

    private final BlizzardAccountService blizzardAccountService;

    public BlizzardController(BlizzardAccountService blizzardAccountService) {
        this.blizzardAccountService = blizzardAccountService;
    }

    @PostMapping("/account/link")
    public BlizzardAccount linkAccount(@Valid @RequestBody BlizzardAccountLinkRequestDto request) {
        return blizzardAccountService.linkBlizzardAccount(
                request.getUserId(),
                request.getBattleTag(),
                request.getAccountId(),
                request.getRegion()
        );
    }
}
