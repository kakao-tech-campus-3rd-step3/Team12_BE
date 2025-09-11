package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.exception.EntityNotFoundException;
import unischedule.users.dto.EventGetResponseDto;
import unischedule.users.entity.Event;
import unischedule.users.repository.EventRepository;

@Service
@RequiredArgsConstructor
public class EventsServiceImpl implements EventsService {
    
    private final EventRepository eventRepository;
    
    @Override
    @Transactional
    public EventGetResponseDto modifyEvent(Long eventId, EventModifyRequestDto requestDto) {
        Event findEvent = eventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("해당 이벤트가 없습니다."));
        
        findEvent.modifyContent(requestDto);
        
        eventRepository.save(findEvent);
        
        return new EventGetResponseDto(findEvent);
    }
}
