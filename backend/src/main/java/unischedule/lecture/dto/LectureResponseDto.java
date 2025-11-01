package unischedule.lecture.dto;

import unischedule.lecture.domain.Lecture;

import java.time.LocalDate;

public record LectureResponseDto(
        Long lectureId,
        Long eventId,
        String name,
        String professor,
        Integer credit,
        LocalDate startDate,
        LocalDate endDate
) {
    public static LectureResponseDto from(Lecture lecture) {
        return new LectureResponseDto(
                lecture.getLectureId(),
                lecture.getEvent().getEventId(),
                lecture.getName(),
                lecture.getProfessor(),
                lecture.getCredit(),
                lecture.getStartDate(),
                lecture.getEndDate()
        );
    }
}

