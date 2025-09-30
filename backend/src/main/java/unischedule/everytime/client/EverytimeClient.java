package unischedule.everytime.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import unischedule.everytime.dto.external.EverytimeTimetableRawResponseDto;

@Component
@RequiredArgsConstructor
public class EverytimeClient {

    private static final String TIMETABLE_URI = "/find/timetable/table/friend";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final WebClient everytimeWebClient;

    public Mono<EverytimeTimetableRawResponseDto> fetchTimetable(String identifier) {
        return everytimeWebClient.post()
                .uri(TIMETABLE_URI)
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .body(BodyInserters.fromFormData("identifier", identifier)
                        .with("friendInfo", "true"))
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new RuntimeException(
                                "Everytime API 호출 실패: " + response.statusCode())))
                .bodyToMono(EverytimeTimetableRawResponseDto.class);
    }
}
