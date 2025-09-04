package unischedule.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import unischedule.users.dto.EventCreateRequestDto;
import unischedule.users.dto.EventCreateResponseDto;
import unischedule.users.dto.EventGetRequestDto;
import unischedule.users.dto.EventGetResponseDto;
import unischedule.users.entity.Event;
import unischedule.users.repository.CalendarRepository;
import unischedule.users.repository.EventRepository;

@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {
    private final CalendarRepository calendarRepository;
    private final EventRepository eventRepository;
    
    //아직 유저 정보를 받아올 수 없기 때문에, 임의로 채움. 추후 유저 구현 확인 후 수정 필요
    @Override
    public EventCreateResponseDto makeEvent(EventCreateRequestDto requestDto) {
        
        Event newEvent = Event.builder()
            .creatorId(1L)
            .title(requestDto.title())
            .content(requestDto.description())
            .startAt(requestDto.startTime())
            .endAt(requestDto.endTime())
            .state("CONFIRMED")
            .isPrivate(requestDto.isPrivate())
            .build();
        
        Event saved = eventRepository.save(newEvent);
        
        return new EventCreateResponseDto(
            saved.getId(),
            1L,
            saved.getTitle(),
            saved.getContent(),
            saved.getStartAt(),
            saved.getEndAt(),
            saved.getIsPrivate(),
            null
        );
    }
    
    @Override
    public EventGetResponseDto getEvent(EventGetRequestDto requestDto) {
        return null;
    }
}
