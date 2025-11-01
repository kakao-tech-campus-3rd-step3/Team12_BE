package unischedule.lecture.everytime.dto;

import jakarta.validation.constraints.Pattern;

public record TimetableDetailRequestDto(
        @Pattern(regexp = "^[A-Za-z0-9]{20}$", message = "유효하지 않은 에브리타임 식별자입니다.")
        String identifier
) {

}

