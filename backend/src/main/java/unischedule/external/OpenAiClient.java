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
                    "model", "gpt-4.1-mini",
                    "input", List.of(
                            Map.of("role", "user", "content", List.of(
                                    Map.of("type", "input_text", "text",
                                            """
                                                    너는 시간표 스크린샷 이미지를 입력받아 사용자에게 JSON으로 변환해주는 역할을 가지고 있어.
                                                      이미지는 표로 구성되어 있고, 행은 시간을 나타내며, 열은 요일(월~금)을 나타낸다.
                                                      입력받은 시간표 이미지를 기반으로, 아래 JSON 구조에 맞춰 내용을 정확하게 추출하여 변환한다.
                                                    
                                                      ### 변환 규칙
                                                    
                                                      1. JSON은 다음 구조를 따른다.
                                                    
                                                      {
                                                        "startDate": "",
                                                        "endDate": "",
                                                        "timetable": {
                                                          "year": "",
                                                          "semester": "",
                                                          "subjects": [
                                                            {
                                                              "name": "과목명",
                                                              "professor": "교수명",
                                                              "credit": 3,
                                                              "times": [
                                                                {
                                                                  "dayOfWeek": 1,
                                                                  "startTime": "09:00:00",
                                                                  "endTime": "10:15:00",
                                                                  "place": "201-XXXX"
                                                                }
                                                              ]
                                                            }
                                                          ]
                                                        }
                                                      }
                                                    
                                                      단, `"startDate"`, `"endDate"`, `"year"`, `"semester"` 값은 이미지에 관련 정보가 없을 경우 빈 문자열("")로 두어도 된다.
                                                    
                                                      2. `"dayOfWeek"`는 월요일부터 1로 시작한다. (월=1, 화=2, 수=3, 목=4, 금=5)
                                                    
                                                      3. `"startTime"`, `"endTime"`은 `"HH:MM:SS"` 형식으로 표현하고, 24시간제를 사용한다. (예: 09:00:00 / 13:30:00)
                                                    
                                                      4. 동일한 과목명 + 교수명 + 강의실은 하나의 `"subject"`로 묶고
                                                         `"times"` 리스트 안에 여러 요일별 시간 정보를 추가한다.
                                                    
                                                      5. `"credit"` 값은 명시되어 있지 않다면 일반 이론 과목은 3, 실습·실험 과목(‘실험’, ‘실습’, ‘프로젝트’, ‘캡스톤’ 등 포함)은 2로 추정한다.
                                                    
                                                      6. 이미지에 시간이 직접 표시되지 않는다면, 표의 격자 간격을 기준으로 상대적 시간대를 추정한다. (예: 한 칸=1시간이면 09:00–10:00 등)
                                                    
                                                      7. 모든 문자열은 정확히 이미지에서 보이는 그대로 입력한다. (과목명, 교수명, 강의실 번호, 요일 등)
                                                    
                                                      8. JSON 외의 다른 설명이나 텍스트는 절대 출력하지 않는다. 오직 JSON만 출력할 것
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
                            String content = resp.get("output").get(0).get("content").get(0)
                                    .get("text").textValue();
                            TimetableDetailDto result = objectMapper.readValue(content,
                                    TimetableDetailDto.class);
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
