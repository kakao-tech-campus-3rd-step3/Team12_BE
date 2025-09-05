package unischedule.everytime.dto;

import java.time.LocalTime;
import java.util.List;
import unischedule.everytime.dto.external.EverytimeTimetableRawResponseDto;
import unischedule.everytime.dto.external.EverytimeTimetableRawResponseDto.Attr;

public record TimetableDetailDto(
        String year,
        String semester,
        List<Subject> subjects
) {

    public record Subject(
            String name,
            String professor,
            Integer credit,
            List<Time> times
    ) {

        private static Integer parseCredit(Attr credit) {
            if (credit == null || credit.value() == null) {
                return null;
            }

            try {
                return Integer.parseInt(credit.value());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public static Subject from(EverytimeTimetableRawResponseDto.Subject rawResponseSubject) {
            return new Subject(
                    rawResponseSubject.name().value(),
                    rawResponseSubject.professor().value(),
                    parseCredit(rawResponseSubject.credit()),
                    rawResponseSubject.time().data().stream().map(Time::from).toList()
            );
        }

        public record Time(
                Integer dayOfWeek,
                LocalTime startTime,
                LocalTime endTime,
                String place
        ) {

            // 에브리타임 API가 09:00를 기준으로 5분 단위로 증가하는 형태로 반환 (ex: 09:00=0, 09:05=1, ..., 18:00=108)
            private static final LocalTime BASE_TIME = LocalTime.of(9, 0);
            private static final long TIME_UNIT_MINUTES = 5L;

            public static Time from(EverytimeTimetableRawResponseDto.TimeData rawResponseTimeData) {
                return new Time(
                        rawResponseTimeData.day(),
                        BASE_TIME.plusMinutes(rawResponseTimeData.starttime() * TIME_UNIT_MINUTES),
                        BASE_TIME.plusMinutes(rawResponseTimeData.endtime() * TIME_UNIT_MINUTES),
                        rawResponseTimeData.place()
                );
            }
        }
    }

    public static TimetableDetailDto from(EverytimeTimetableRawResponseDto.Table rawResponseTable) {
        return new TimetableDetailDto(
                rawResponseTable.year(),
                rawResponseTable.semester(),
                rawResponseTable.subject().stream().map(Subject::from).toList()
        );
    }
}
