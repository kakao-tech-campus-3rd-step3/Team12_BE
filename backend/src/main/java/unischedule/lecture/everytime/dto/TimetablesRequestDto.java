package unischedule.lecture.everytime.dto;

import jakarta.validation.constraints.Pattern;

public record TimetablesRequestDto(
        @Pattern(regexp = "^https://everytime\\.kr/@[A-Za-z0-9]{20}$", message = "유효하지 않은 에브리타임 URL입니다.")
        String url
) {

}

