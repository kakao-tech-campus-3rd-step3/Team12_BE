package unischedule.lecture.dto;

import unischedule.lecture.everytime.dto.TimetableDetailDto;

import java.util.List;

public record LectureCreateResponseDto(
        Long lectureId,
        Long eventId,
        String name,
        String professor,
        Integer credit,
        List<TimetableDetailDto.Subject.Time> times
) {
}

