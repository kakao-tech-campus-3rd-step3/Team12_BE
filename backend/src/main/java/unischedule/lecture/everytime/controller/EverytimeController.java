package unischedule.lecture.everytime.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import unischedule.lecture.everytime.dto.TimetableDetailDto;
import unischedule.lecture.everytime.dto.TimetableDetailRequestDto;
import unischedule.lecture.everytime.dto.TimetableDto;
import unischedule.lecture.everytime.dto.TimetablesRequestDto;
import unischedule.lecture.everytime.service.EverytimeService;

import java.util.List;

@RestController
@RequestMapping("/api/everytime")
@RequiredArgsConstructor
@Validated
public class EverytimeController {

    private final EverytimeService everytimeService;

    @GetMapping("/timetables")
    public ResponseEntity<List<TimetableDto>> getTimetables(
            @ModelAttribute @Valid TimetablesRequestDto timetablesRequestDto) {
        return ResponseEntity.ok(everytimeService.getTimetables(timetablesRequestDto.url()));
    }

    @GetMapping("/timetable")
    public ResponseEntity<TimetableDetailDto> getTimetableDetail(
            @ModelAttribute @Valid TimetableDetailRequestDto timetableDetailRequestDto) {
        return ResponseEntity.ok(everytimeService.getTimetableDetail(timetableDetailRequestDto.identifier()));
    }

    @PostMapping("/timetable")
    public ResponseEntity<TimetableDetailDto> getTimetableDetailFromImage(
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(everytimeService.getTimetableDetailFromImage(image));
    }
}

