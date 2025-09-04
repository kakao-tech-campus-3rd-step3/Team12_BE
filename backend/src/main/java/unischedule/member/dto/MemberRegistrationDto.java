package unischedule.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberRegistrationDto(
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        String email,
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,
        @Size(min = 8, message = "비밀번호는 최소 8자리 이상이어야 합니다.")
        String password
) {
}
