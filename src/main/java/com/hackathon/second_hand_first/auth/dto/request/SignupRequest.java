package com.hackathon.second_hand_first.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

        @NotBlank(message = "이름은 필수입니다.")
        String name,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,

        @AssertTrue(message = "이용약관에 동의해야 합니다.")
        boolean termsAgreed,

        boolean marketingConsent,
        String profileImageUrl
) {
}
