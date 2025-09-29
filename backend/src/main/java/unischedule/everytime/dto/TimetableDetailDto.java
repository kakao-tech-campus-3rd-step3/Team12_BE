package unischedule.everytime.dto;

import java.time.LocalTime;
import java.util.List;

public record TimetableDetailDto(
        String year,
        String semester,
        List<Subject> subjects
) {

    public static TimetableDetailDto of(String year, String semester, List<Subject> subjects) {
        return new TimetableDetailDto(year, semester, subjects);
    }

    public record Subject(
            String name,
            String professor,
            Integer credit,
            List<Time> times
    ) {

        public static Subject from(
                String name, String professor, Integer credit, List<Time> times) {
            return new Subject(name, professor, credit, times);
        }

        public record Time(
                Integer dayOfWeek,
                LocalTime startTime,
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
