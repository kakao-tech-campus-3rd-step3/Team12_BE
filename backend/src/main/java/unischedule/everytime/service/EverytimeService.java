package unischedule.everytime.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import unischedule.exception.EntityNotFoundException;
import unischedule.external.OpenAiClient;
import unischedule.everytime.dto.TimetableDetailDto;
import unischedule.everytime.dto.TimetableDto;
import unischedule.external.dto.EverytimeTimetableRawResponseDto;
import unischedule.external.EverytimeClient;
import unischedule.everytime.mapper.EverytimeTimetableMapper;
import unischedule.exception.ExternalApiException;

@Service
@RequiredArgsConstructor
public class EverytimeService {

    private final OpenAiClient openAiClient;
    private final EverytimeClient everytimeClient;
    private final EverytimeTimetableMapper everytimeTimetableMapper;

    public List<TimetableDto> getTimetables(String url) {
        String identifier = extractIdentifierFromEverytimeUrl(url);
        EverytimeTimetableRawResponseDto rawResponse = getTimetableData(identifier);
        return everytimeTimetableMapper.toTimetableDtos(rawResponse);
    }

    public TimetableDetailDto getTimetableDetail(String identifier) {
        EverytimeTimetableRawResponseDto rawResponse = getTimetableData(identifier);
        return everytimeTimetableMapper.toTimetableDetailDto(rawResponse);
    }

    public TimetableDetailDto getTimetableDetailFromImage(MultipartFile image) {
        return openAiClient.extractFromImage(image).block();
    }

    private EverytimeTimetableRawResponseDto getTimetableData(String identifier) {
        EverytimeTimetableRawResponseDto response = everytimeClient.fetchTimetable(identifier)
                .blockOptional()
                .orElseThrow(() -> new ExternalApiException("에브리타임 API 호출에 실패했습니다."));

        if (response.primaryTables() == null || response.primaryTables().primaryTable().isEmpty() ||
                response.table() == null) {
            throw new EntityNotFoundException("해당 시간표를 찾을 수 없습니다.");
        }

        return response;
    }

    private String extractIdentifierFromEverytimeUrl(String urlString) {
        return urlString.substring(urlString.lastIndexOf("@") + 1);
    }
}
