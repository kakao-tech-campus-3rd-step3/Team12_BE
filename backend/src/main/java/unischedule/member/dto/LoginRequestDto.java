package unischedule.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        String email,
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
