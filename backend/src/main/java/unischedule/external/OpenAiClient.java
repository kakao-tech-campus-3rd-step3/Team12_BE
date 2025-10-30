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
                                                      너는 시간표 스크린샷 이미지를 입력받아 사용자에게 json으로 변환해주는 역할을 가지고 있어.
                                                      이미지는 표로 구성되어있고, 행은 시간을 나타내고, 열은 요일을 나타내.
                                                      입력받은 시간표 이미지를 표에 표시된 시간과 요일 및 아래 사항들에 유의하여 JSON으로 정확하게 변환해줘.
                                                    
                                                      1. 과목명, 교수, 요일, 시작/끝 시간, 장소 포함.
                                                      2. 수업 시작 시간, 끝 시간은 정각이 아닐 수도 있어. 5분 간격임.
                                                      3. 보이는 그대로 작성할 것. dayOfWeek는 월요일이 0임.
                                                      4. 시간은 01:01 형식 유지. 시간 분 모두 2자리로 고정할 것. 24시간제 사용.
                                                      5. 동일 수업(동일 색상)에 시간이 여러 개 있는 경우도 있어. 해당 경우 하나의 수업에 시간 list를 추가하는 방식으로 응답해야 함.
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
