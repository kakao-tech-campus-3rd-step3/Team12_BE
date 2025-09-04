package unischedule.users.service;

import java.time.LocalDateTime;
import java.util.List;
import unischedule.users.dto.EventCreateRequestDto;
import unischedule.users.dto.EventCreateResponseDto;
import unischedule.users.dto.EventGetRequestDto;
import unischedule.users.dto.EventGetResponseDto;

public interface UsersService {
    
    EventCreateResponseDto makeEvent(Long userId, EventCreateRequestDto requestDto);
    
    List<EventGetResponseDto> getEvents(LocalDateTime startAt, LocalDateTime endAt, Long userId);
}
