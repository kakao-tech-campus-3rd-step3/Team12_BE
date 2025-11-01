package unischedule.lecture.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import unischedule.lecture.everytime.dto.TimetableDetailDto;

import java.time.LocalDate;

public record LecturesCreateRequestDto(
        @NotNull(message = "강의 시작일은 필수입니다")
        LocalDate startDate,
        
        @NotNull(message = "강의 종료일은 필수입니다")
        LocalDate endDate,
        
        @NotNull(message = "시간표 정보는 필수입니다")
        @Valid
        TimetableDetailDto timetable
) {
}

