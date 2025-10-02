package unischedule.events.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.service.PersonalEventService;
import unischedule.events.dto.EventGetResponseDto;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class PersonalEventController {
    private final PersonalEventService eventService;

    @PostMapping("/add")
    public ResponseEntity<EventCreateResponseDto> makeMyEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @RequestBody
            PersonalEventCreateRequestDto requestDto
    ) {
        EventCreateResponseDto responseDto = eventService.makePersonalEvent(userDetails.getUsername(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<EventGetResponseDto>> getMyEvents(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        List<EventGetResponseDto> responseDto = eventService.getPersonalEvents(userDetails.getUsername(), startAt, endAt);
        return ResponseEntity.ok(responseDto);
    }
    
    @PatchMapping("/modify")
    public ResponseEntity<EventGetResponseDto> modifyMyEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @RequestBody
            EventModifyRequestDto requestDto
    ) {
        log.error(userDetails.getUsername());
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
    
    @GetMapping("/upcomming")
    public ResponseEntity<List<EventGetResponseDto>> upcomingMyEvent(
        @AuthenticationPrincipal
        UserDetails userDetails
    ) {
        List<EventGetResponseDto> upcomingList = eventService.getUpcomingMyEvent(userDetails.getUsername());
        return ResponseEntity.ok(upcomingList);
    }
    
    @GetMapping("/today")
    public ResponseEntity<List<EventGetResponseDto>> todayMyEvent(
        @AuthenticationPrincipal
        UserDetails userDetails
    ) {
        List<EventGetResponseDto> upcomingList = eventService.getTodayMyEvent(userDetails.getUsername());
        return ResponseEntity.ok(upcomingList);
    }
}
