package com.gamematcher.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AuthDto {

    @Getter @Setter @NoArgsConstructor
    public static class SignupRequest {
        @NotBlank(message = "아이디는 필수입니다.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
        private String loginId;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 4, max = 50, message = "비밀번호는 4자 이상이어야 합니다.")
        private String password;

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        private String username;

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        private String email;
    }

    @Getter @Setter @NoArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "아이디는 필수입니다.")
        private String loginId;

        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;
    }

    @Getter @Setter
    public static class LoginResponse {
        private Long userId;
        private String loginId;
        private String username;
        private String email;
        private String role;
        private String authToken;

        public LoginResponse(Long userId, String loginId, String username, String email, String role, String authToken) {
            this.userId = userId;
            this.loginId = loginId;
            this.username = username;
            this.email = email;
            this.role = role;
            this.authToken = authToken;
        }
    }
}
