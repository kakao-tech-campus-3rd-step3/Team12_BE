package unischedule.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import unischedule.lecture.everytime.dto.TimetableDetailDto;
import unischedule.exception.ExternalApiException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;

    public Mono<TimetableDetailDto> extractFromImage(MultipartFile file) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());
            Map<String, Object> request = Map.of(
                    "model", "gpt-5-nano",
                    "reasoning", Map.of("effort", "low"),
                    "input", List.of(
                            Map.of("role", "user", "content", List.of(
                                    Map.of("type", "input_text", "text",
                                                """
                                                      이 시간표 이미지를 표에 표시된 시간과 요일에 유의하여 JSON으로 변환해줘.
                                                      과목명, 교수, 요일, 시작/끝 시간, 장소 포함. 수업 시작 시간, 끝 시간은 정각이 아닐 수도 있어.
                                                      보이는 그대로 작성할 것. dayOfWeek는 월요일이 0임.
                                                      시간은 01:01 형식 유지. 시간 분 모두 2자리로 고정할 것. 24시간제 사용.
                                                      동일 수업(동일 색상)에 시간이 여러개 있는 경우도 있어. 해당 경우 하나의 수업에 시간 list를 추가하는 방식으로 응답해야함.
                                                    """),
                                    Map.of("type", "input_image", "image_url",
                                            "data:image/png;base64," + base64Image))
                            )
                    ),
                    "text", Map.of("format",
                            Map.of(
                                    "type", "json_schema",
                                    "name", "timetable_schema",
                                    "schema", createTimetableScheme(),
                                    "strict", true
                            )
                    )
            );

            return openAiWebClient.post()
                    .uri("/responses")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .handle((resp, sink) -> {
                        try {
                            String content = resp.get("output").get(1).get("content").get(0).get("text").textValue();
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
