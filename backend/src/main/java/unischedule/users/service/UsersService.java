package unischedule.users.service;

import java.util.List;
import unischedule.users.dto.EventCreateRequestDto;
import unischedule.users.dto.EventCreateResponseDto;
import unischedule.users.dto.EventGetRequestDto;
import unischedule.users.dto.EventGetResponseDto;

public interface UsersService {
    
    EventCreateResponseDto makeEvent(EventCreateRequestDto requestDto);
    
    List<EventGetResponseDto> getEvents(EventGetRequestDto requestDto);
}
