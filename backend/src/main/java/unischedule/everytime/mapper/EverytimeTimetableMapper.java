package unischedule.everytime.mapper;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import unischedule.everytime.dto.TimetableDto;
import unischedule.external.dto.EverytimeTimetableRawResponseDto;

@Component
@RequiredArgsConstructor
public class EverytimeTimetableMapper {

    public List<TimetableDto> toTimetableDtos(EverytimeTimetableRawResponseDto rawResponse) {
        return rawResponse.primaryTables().primaryTable().stream()
                .map(TimetableDto::from)
                .toList();
    }
}
