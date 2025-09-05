package unischedule.everytime.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import unischedule.everytime.dto.TimetableDetailDto;
import unischedule.everytime.dto.TimetableDto;
import unischedule.everytime.dto.external.EverytimeTimetableRawResponseDto;
import unischedule.exception.ExternalApiException;
import unischedule.exception.InvalidInputException;

@Service
@RequiredArgsConstructor
public class EverytimeService {

    private final EverytimeClient everytimeClient;
    private final EverytimeTimetableMapper everytimeTimetableMapper;

    public List<TimetableDto> getTimetables(String identifier) {
        EverytimeTimetableRawResponseDto rawResponse = getTimetableData(identifier);
        return everytimeTimetableMapper.toTimetableDtos(rawResponse);
    }

    public TimetableDetailDto getTimetableDetail(String identifier) {
        EverytimeTimetableRawResponseDto rawResponse = getTimetableData(identifier);
        return TimetableDetailDto.from(rawResponse.table());
    }

    private EverytimeTimetableRawResponseDto getTimetableData(String identifier) {
        EverytimeTimetableRawResponseDto response = everytimeClient.fetchTimetable(identifier)
                .blockOptional()
                .orElseThrow(() -> new ExternalApiException("에브리타임 API 호출에 실패했습니다."));

        if (response.primaryTables() == null || response.primaryTables().primaryTable().isEmpty()) {
            throw new InvalidInputException("해당 시간표를 찾을 수 없습니다.");
        }

        return response;
    }
}

