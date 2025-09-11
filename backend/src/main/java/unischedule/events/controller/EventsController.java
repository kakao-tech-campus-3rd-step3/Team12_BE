package unischedule.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.service.EventsService;
import unischedule.users.dto.EventGetResponseDto;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventsController {
    private final EventsService eventsService;
    
    @PatchMapping("/{eventId}")
    public ResponseEntity<EventGetResponseDto> modifyEvent(
        @PathVariable Long eventId,
        @RequestBody EventModifyRequestDto requestDto
    ) {
        EventGetResponseDto responseDto = eventsService.modifyEvent(eventId, requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
