package unischedule.lecture.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unischedule.lecture.dto.LectureResponseDto;
import unischedule.lecture.dto.LecturesCreateRequestDto;
import unischedule.lecture.dto.LecturesCreateResponseDto;
import unischedule.lecture.service.LectureService;

import java.util.List;

@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class LectureController {
    
    private final LectureService lectureService;
    
    @GetMapping
    public ResponseEntity<List<LectureResponseDto>> getMyLectures(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<LectureResponseDto> responseDto = lectureService.getMyLectures(
                userDetails.getUsername()
        );
        return ResponseEntity.ok(responseDto);
    }
    
    @PutMapping
    public ResponseEntity<LecturesCreateResponseDto> saveLectures(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid LecturesCreateRequestDto requestDto
    ) {
        LecturesCreateResponseDto responseDto = lectureService.saveLectures(
                userDetails.getUsername(),
                requestDto
        );
        return ResponseEntity.ok(responseDto);
    }
}

