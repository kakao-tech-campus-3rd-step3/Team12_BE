package unischedule.everytime.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import unischedule.everytime.dto.TimetableDto;
import unischedule.everytime.dto.external.EverytimeTimetableRawResponseDto;

@Component
@RequiredArgsConstructor
class EverytimeTimetableMapper {

    protected List<TimetableDto> toTimetableDtos(EverytimeTimetableRawResponseDto rawResponse) {
        return rawResponse.primaryTables().primaryTable().stream()
                .map(TimetableDto::from)
                .toList();
    }
}
