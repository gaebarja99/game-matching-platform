package com.gamematcher.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "아이디를 입력하세요")
    @Size(min = 2, max = 50)
    private String loginId;

    @NotBlank(message = "비밀번호를 입력하세요")
    @Size(min = 4, max = 100)
    private String password;

    @NotBlank(message = "이름을 입력하세요")
    @Size(max = 50)
    private String username;

    @Size(max = 50)
    private String nickname;

    @NotBlank(message = "이메일을 입력하세요")
    @Email
    private String email;

    @Size(max = 20)
    private String phone;
}
