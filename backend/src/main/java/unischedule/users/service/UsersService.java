package unischedule.users.service;

import unischedule.users.dto.EventCreateRequestDto;
import unischedule.users.dto.EventCreateResponseDto;
import unischedule.users.dto.EventGetRequestDto;
import unischedule.users.dto.EventGetResponseDto;

public interface UsersService {
    
    EventCreateResponseDto makeEvent(EventCreateRequestDto requestDto);
    
    EventGetResponseDto getEvent(EventGetRequestDto requestDto);
}
