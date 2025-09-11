package unischedule.events.service;

import unischedule.events.dto.EventModifyRequestDto;
import unischedule.users.dto.EventGetResponseDto;

public interface EventsService {
    
    EventGetResponseDto modifyEvent(Long eventId, EventModifyRequestDto requestDto);
}
