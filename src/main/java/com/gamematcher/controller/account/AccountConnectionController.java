package com.gamematcher.controller.account;

import com.gamematcher.dto.account.AccountConnectionsResponseDto;
import com.gamematcher.dto.account.OAuthStartResponseDto;
import com.gamematcher.dto.account.RiotAccountLinkResponseDto;
import com.gamematcher.dto.account.RiotManualLinkRequestDto;
import com.gamematcher.dto.account.RiotVerificationConfirmRequestDto;
import com.gamematcher.dto.account.RiotVerificationStartRequestDto;
import com.gamematcher.dto.account.RiotVerificationStartResponseDto;
import com.gamematcher.exception.GameApiException;
import com.gamematcher.service.account.AccountConnectionService;
import com.gamematcher.service.account.RiotAccountService;
import com.gamematcher.service.account.RiotOwnershipVerificationService;
import com.gamematcher.service.auth.CurrentUserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/account-links")
@RequiredArgsConstructor
public class AccountConnectionController {

    private final AccountConnectionService accountConnectionService;
    private final CurrentUserService currentUserService;
    private final RiotAccountService riotAccountService;
    private final RiotOwnershipVerificationService riotOwnershipVerificationService;

    @GetMapping
    public AccountConnectionsResponseDto getConnections(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken) {
        return accountConnectionService.getConnections(authToken);
    }

    @GetMapping("/user/{userId}")
    public AccountConnectionsResponseDto getConnectionsForUser(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @PathVariable Long userId) {
        return accountConnectionService.getConnectionsForUser(authToken, userId);
    }

    @DeleteMapping("/{provider}")
    public ResponseEntity<Void> unlink(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @PathVariable String provider) {
        accountConnectionService.unlink(authToken, provider);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/oauth/{provider}/start")
    public OAuthStartResponseDto startOAuth(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @PathVariable String provider) {
        return switch (provider.toLowerCase()) {
            case "discord" -> accountConnectionService.startDiscord(authToken);
            case "steam" -> accountConnectionService.startSteam(authToken);
            case "blizzard" -> accountConnectionService.startBlizzard(authToken);
            default -> throw new GameApiException(HttpStatus.BAD_REQUEST, "지원하지 않는 연동 제공자입니다: " + provider);
        };
    }

    @PostMapping("/riot/manual")
    public RiotAccountLinkResponseDto linkRiotManually(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @Valid @RequestBody RiotManualLinkRequestDto request) {
        Long userId = currentUserService.requireUser(authToken).getId();
        return riotAccountService.linkAccount(userId, request.getGameName(), request.getTagLine());
    }

    @PostMapping("/riot/verification/start")
    public RiotVerificationStartResponseDto startRiotVerification(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @Valid @RequestBody RiotVerificationStartRequestDto request) {
        Long userId = currentUserService.requireUser(authToken).getId();
        return riotOwnershipVerificationService.startVerification(userId, request);
    }

    @PostMapping("/riot/verification/confirm")
    public RiotAccountLinkResponseDto confirmRiotVerification(
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @Valid @RequestBody RiotVerificationConfirmRequestDto request) {
        Long userId = currentUserService.requireUser(authToken).getId();
        return riotOwnershipVerificationService.confirmVerification(userId, request);
    }

    @GetMapping("/oauth/discord/callback")
    public void discordCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {
        response.sendRedirect(accountConnectionService.handleDiscordCallback(code, state, error));
    }

    @GetMapping("/oauth/blizzard/callback")
    public void blizzardCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {
        response.sendRedirect(accountConnectionService.handleBlizzardCallback(code, state, error));
    }

    @GetMapping("/oauth/steam/callback")
    public void steamCallback(
            @RequestParam Map<String, String> params,
            HttpServletResponse response) throws IOException {
        response.sendRedirect(accountConnectionService.handleSteamCallback(params));
    }

    @GetMapping(value = "/oauth/result", produces = MediaType.TEXT_HTML_VALUE)
    public String oauthResultPage(
            @RequestParam String provider,
            @RequestParam String result,
            @RequestParam String message,
            @RequestParam String next) {
        String tone = "success".equalsIgnoreCase(result) ? "#16a34a" : "#dc2626";
        String title = "success".equalsIgnoreCase(result) ? "계정 연동 완료" : "계정 연동 실패";
        String safeProvider = escapeHtml(provider);
        String safeMessage = escapeHtml(message);
        String safeNext = escapeHtml(next);
        String nextForJs = escapeJs(next);
        String providerForJs = escapeJs(provider);
        String resultForJs = escapeJs(result);
        String messageForJs = escapeJs(message);

        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>%s</title>
                  <style>
                    body {
                      margin: 0;
                      min-height: 100vh;
                      display: grid;
                      place-items: center;
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                      background: linear-gradient(180deg, #f8fafc, #eef2ff);
                      color: #0f172a;
                    }
                    .card {
                      width: min(92vw, 480px);
                      padding: 32px;
                      border-radius: 28px;
                      background: rgba(255, 255, 255, 0.92);
                      box-shadow: 0 20px 50px rgba(148, 163, 184, 0.18);
                      border: 1px solid rgba(255, 255, 255, 0.88);
                    }
                    .badge {
                      display: inline-flex;
                      padding: 7px 12px;
                      border-radius: 999px;
                      background: rgba(255,255,255,0.9);
                      color: %s;
                      font-weight: 700;
                      margin-bottom: 14px;
                    }
                    h1 { margin: 0 0 10px; font-size: 28px; }
                    p { margin: 0; line-height: 1.7; color: #475569; }
                    .actions { display: flex; gap: 10px; margin-top: 24px; flex-wrap: wrap; }
                    a, button {
                      appearance: none;
                      border: 0;
                      border-radius: 14px;
                      padding: 12px 16px;
                      font: inherit;
                      font-weight: 700;
                      cursor: pointer;
                      text-decoration: none;
                    }
                    .primary { background: %s; color: white; }
                    .secondary { background: #e2e8f0; color: #0f172a; }
                    .hint { margin-top: 16px; font-size: 13px; color: #64748b; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="badge">%s</div>
                    <h1>%s</h1>
                    <p>%s</p>
                    <div class="actions">
                      <a class="primary" href="%s">연동 화면으로 이동</a>
                      <button class="secondary" type="button" onclick="window.close()">창 닫기</button>
                    </div>
                    <p class="hint">팝업이 자동으로 닫히지 않으면 버튼으로 직접 돌아가면 됩니다.</p>
                  </div>
                  <script>
                    const next = "%s";
                    const payload = {
                      type: "ACCOUNT_LINK_RESULT",
                      provider: "%s",
                      result: "%s",
                      message: "%s"
                    };

                    if (window.opener && !window.opener.closed) {
                      try {
                        window.opener.postMessage(payload, "*");
                      } catch (error) {
                        console.error(error);
                      }
                      window.close();
                    } else {
                      window.location.replace(next);
                    }
                  </script>
                </body>
                </html>
                """.formatted(
                title,
                tone,
                tone,
                safeProvider.toUpperCase(),
                title,
                safeMessage,
                safeNext,
                nextForJs,
                providerForJs,
                resultForJs,
                messageForJs
        );
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeJs(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "");
    }
}
