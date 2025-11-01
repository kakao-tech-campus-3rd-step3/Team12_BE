package unischedule.lecture.everytime.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record TimetableDetailDto(
        String year,
        String semester,

        @Valid
        List<Subject> subjects
) {

    public static TimetableDetailDto of(String year, String semester, List<Subject> subjects) {
        return new TimetableDetailDto(year, semester, subjects);
    }

    public record Subject(
            @NotBlank(message = "강의명은 필수입니다")
            String name,
            
            @NotBlank(message = "교수명은 필수입니다")
            String professor,

            Integer credit,
            
            @NotEmpty(message = "강의 시간은 최소 1개 이상이어야 합니다")
            @Valid
            List<Time> times
    ) {

        public static Subject from(
                String name, String professor, Integer credit, List<Time> times) {
            return new Subject(name, professor, credit, times);
        }

        public record Time(
                @NotNull(message = "요일은 필수입니다")
                @Min(value = 1, message = "요일은 1(월)부터 7(일) 사이여야 합니다")
                @Max(value = 7, message = "요일은 1(월)부터 7(일) 사이여야 합니다")
                Integer dayOfWeek,
                
                @NotNull(message = "시작 시간은 필수입니다")
                LocalTime startTime,
                
                @NotNull(message = "종료 시간은 필수입니다")
                LocalTime endTime,

                String place
        ) {

            public static Time from(
                    Integer dayOfWeek, LocalTime startTime, LocalTime endTime, String place) {
                return new Time(dayOfWeek, startTime, endTime, place);
            }
        }
    }
}

