package unischedule.lecture.everytime.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import unischedule.lecture.everytime.dto.TimetableDetailDto;
import unischedule.lecture.everytime.dto.TimetableDto;
import unischedule.external.dto.EverytimeTimetableRawResponseDto;
import unischedule.external.dto.EverytimeTimetableRawResponseDto.Attr;

import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EverytimeTimetableMapper {

    private static final LocalTime BASE_TIME = LocalTime.of(0, 0);
    private static final long TIME_UNIT_MINUTES = 5L;

    public List<TimetableDto> toTimetableDtos(EverytimeTimetableRawResponseDto rawResponse) {
        if (rawResponse.primaryTables() == null || rawResponse.primaryTables().primaryTable() == null) {
            return List.of();
        }
        
        return rawResponse.primaryTables().primaryTable().stream()
                .map(TimetableDto::from)
                .toList();
    }

    public TimetableDetailDto toTimetableDetailDto(EverytimeTimetableRawResponseDto rawResponse) {
        if (rawResponse.table() == null) {
            return TimetableDetailDto.of(null, null, List.of());
        }

        EverytimeTimetableRawResponseDto.Table table = rawResponse.table();
        return TimetableDetailDto.of(
                table.year(),
                table.semester(),
                table.subject() == null ? List.of()
                        : table.subject().stream().map(this::mapToSubject).toList()
        );
    }

    private TimetableDetailDto.Subject mapToSubject(
            EverytimeTimetableRawResponseDto.Subject rawSubject) {
        return TimetableDetailDto.Subject.from(
                extractAttrValue(rawSubject.name()),
                extractAttrValue(rawSubject.professor()),
                parseCredit(rawSubject.credit()),
                extractTimes(rawSubject.time())
        );
    }

    private Integer parseCredit(Attr credit) {
        if (credit == null || credit.value() == null) {
            return null;
        }

        try {
            return Integer.parseInt(credit.value());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractAttrValue(Attr attr) {
        return attr != null ? attr.value() : null;
    }

    private List<TimetableDetailDto.Subject.Time> extractTimes(EverytimeTimetableRawResponseDto.Time time) {
        if (time == null || time.data() == null) {
            return List.of();
        }
        return time.data().stream().map(this::mapToTime).toList();
    }


    private TimetableDetailDto.Subject.Time mapToTime(
            EverytimeTimetableRawResponseDto.TimeData rawTimeData) {
        return TimetableDetailDto.Subject.Time.from(
                rawTimeData.day() + 1,
                BASE_TIME.plusMinutes(rawTimeData.starttime() * TIME_UNIT_MINUTES),
                BASE_TIME.plusMinutes(rawTimeData.endtime() * TIME_UNIT_MINUTES),
                rawTimeData.place()
        );
    }
}

