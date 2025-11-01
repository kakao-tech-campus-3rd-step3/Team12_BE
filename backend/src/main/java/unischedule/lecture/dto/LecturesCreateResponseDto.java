package unischedule.lecture.dto;

import java.util.List;

public record LecturesCreateResponseDto(
        List<LectureCreateResponseDto> lectures
) {
}

