package unischedule.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import unischedule.events.dto.EventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.service.EventService;
import unischedule.events.dto.EventGetResponseDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping("/add")
    public ResponseEntity<EventCreateResponseDto> makeMyEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @RequestBody
            EventCreateRequestDto requestDto
    ) {
        EventCreateResponseDto responseDto = eventService.makePersonalEvent(userDetails.getUsername(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
    //추후 실제 테스트 때 들어오는 일정의 형식에 따라
    //Dto 내부의 데이터를 일부 파싱해야할 가능성 있음

    @GetMapping
    public ResponseEntity<List<EventGetResponseDto>> getMyEvents(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startAt,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endAt
    ) {
        // 날짜 → LocalDateTime 변환
        LocalDateTime startDateTime = startAt.atStartOfDay();           // 00:00
        LocalDateTime endDateTime = endAt.atTime(LocalTime.MAX);        // 23:59:59.999999999

        List<EventGetResponseDto> responseDto = eventService.getPersonalEvents(userDetails.getUsername(), startDateTime, endDateTime);
        return ResponseEntity.ok(responseDto);
    }
    //현재는 태그 없이 바로 리스트형태 반환
    //추후 태그 필요 시 태그를 포함하는 Dto 형태로 다시 작성할 필요 있음
    
    @PatchMapping("/modify")
    public ResponseEntity<EventGetResponseDto> modifyMyEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @RequestBody
            EventModifyRequestDto requestDto
    ) {
        EventGetResponseDto responseDto = eventService.modifyPersonalEvent(userDetails.getUsername(), requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteMyEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @PathVariable
            Long eventId
    ) {
        eventService.deletePersonalEvent(userDetails.getUsername(), eventId);
        return ResponseEntity.noContent().build();
    }

//    @GetMapping("/{userId}/today")
//    public ResponseEntity<List<EventGetResponseDto>> todayEvent() {
//
//    }
}
