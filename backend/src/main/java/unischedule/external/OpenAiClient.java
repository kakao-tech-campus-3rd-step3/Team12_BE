package unischedule.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import unischedule.everytime.dto.TimetableDetailDto;
import unischedule.external.dto.OpenAiChatCompletionResponseDto;
import unischedule.exception.ExternalApiException;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    public Mono<TimetableDetailDto> extractFromImage(MultipartFile file) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            Map<String, Object> request = Map.of(
                    "model", "gpt-4.1-nano",
                    "messages", List.of(
                            Map.of("role", "user", "content", List.of(
                                    Map.of("type", "text", "text",
                                            "이 시간표 이미지를 JSON으로 변환해줘. 과목명, 교수, 요일, 시작/끝 시간, 장소 포함. 시작 시간, 끝 시간은 정각이 아닐 수도 있어. dayOfWeek는 월요일이 0임."),
                                    Map.of("type", "image_url", "image_url",
                                            Map.of("url", "data:image/png;base64," + base64Image))
                            ))
                    ),
                    "response_format", Map.of(
                            "type", "json_schema",
                            "json_schema", Map.of(
                                    "name", "timetable_schema",
                                    "schema", createTimetableScheme(),
                                    "strict", true
                            )
                    )
            );

            return openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAiChatCompletionResponseDto.class)
                    .handle((resp, sink) -> {
                        try {
                            String content = resp.choices().getFirst().message().content();
                            TimetableDetailDto result = objectMapper.readValue(content, TimetableDetailDto.class);
                            sink.next(result);
                        } catch (JsonProcessingException e) {
                            sink.error(new ExternalApiException("OpenAI API 응답 처리 실패"));
                        }
                    });

        } catch (Exception e) {
            throw new ExternalApiException("OpenAI API 요청 실패");
        }
    }

    private Map<String, Object> createTimetableScheme() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "year", Map.of("type", "string"),
                        "semester", Map.of("type", "string"),
                        "subjects", Map.of(
                                "type", "array",
                                "items", Map.of(
                                        "type", "object",
                                        "properties", Map.of(
                                                "name", Map.of("type", "string"),
                                                "professor", Map.of("type", "string"),
                                                "credit", Map.of("type", "integer"),
                                                "times", Map.of(
                                                        "type", "array",
                                                        "items", Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                        "dayOfWeek",
                                                                        Map.of("type", "integer"),
                                                                        "startTime",
                                                                        Map.of("type", "string"),
                                                                        "endTime",
                                                                        Map.of("type", "string"),
                                                                        "place",
                                                                        Map.of("type", "string")
                                                                ),
                                                                "required",
                                                                List.of("dayOfWeek", "startTime",
                                                                        "endTime", "place"),
                                                                "additionalProperties", false
                                                        )
                                                )
                                        ),
                                        "required", List.of("name", "professor", "credit", "times"),
                                        "additionalProperties", false
                                )
                        )
                ),
                "required", List.of("year", "semester", "subjects"),
                "additionalProperties", false
        );
    }
}
