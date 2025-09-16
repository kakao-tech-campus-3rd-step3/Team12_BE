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
import unischedule.events.dto.EventGetResponseDto;

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
    
    //아래 부분은 테크 스펙에 대한 정리가 있고 난 이후에나 구현 가능할 듯
//    @GetMapping("/{userId}/today")
//    public ResponseEntity<List<EventGetResponseDto>> todayEvent() {
//
//    }
}
