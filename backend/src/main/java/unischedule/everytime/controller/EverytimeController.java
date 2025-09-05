package unischedule.everytime.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import unischedule.everytime.dto.TimetableDetailDto;
import unischedule.everytime.dto.TimetableDto;
import unischedule.everytime.service.EverytimeService;

@RestController
@RequestMapping("/api/everytime")
@RequiredArgsConstructor
@Validated
public class EverytimeController {

    private final EverytimeService everytimeService;

    @GetMapping("/timetables")
    public ResponseEntity<List<TimetableDto>> getTimetables(
            @RequestParam
            @Pattern(regexp = "^https://everytime\\.kr/@[A-Za-z0-9]{20}$", message = "유효하지 않은 에브리타임 URL입니다.")
            String url) {
        String identifier = getIdentifierFromEverytimeUrl(url);
        return ResponseEntity.ok(everytimeService.getTimetables(identifier));
    }

    @GetMapping("/timetable")
    public ResponseEntity<TimetableDetailDto> getTimetableDetail(
            @RequestParam
            @Pattern(regexp = "^[A-Za-z0-9]{20}$", message = "유효하지 않은 에브리타임 식별자입니다.")
            String identifier) {
        return ResponseEntity.ok(everytimeService.getTimetableDetail(identifier));
    }

    private String getIdentifierFromEverytimeUrl(String urlString) {
        return urlString.substring(urlString.lastIndexOf("@") + 1);
    }
}
