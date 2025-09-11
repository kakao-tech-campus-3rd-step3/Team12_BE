package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.users.dto.EventGetResponseDto;
import unischedule.users.entity.Event;
import unischedule.users.repository.EventRepository;

@Service
@RequiredArgsConstructor
public class EventsService {
    
    private final EventRepository eventRepository;
    
    @Transactional
    public EventGetResponseDto modifyEvent(Long eventId, EventModifyRequestDto requestDto) {
        Event findEvent = eventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("해당 이벤트가 없습니다."));
        
        if (requestDto.startTime() != null || requestDto.endTime() != null) {
            boolean conflict = eventRepository.existsByCreatorIdAndIdNotAndStartAtLessThanAndEndAtGreaterThan(
                findEvent.getCreatorId(),
                eventId,
                requestDto.endTime() != null ? requestDto.endTime() : findEvent.getEndAt(),
                requestDto.startTime() != null ? requestDto.startTime() : findEvent.getStartAt()
            );
            
            if (conflict) {
                throw new InvalidInputException("해당 시간에 겹치는 일정이 있어 수정할 수 없습니다.");
            }
        }
        
        findEvent.modifyContent(requestDto);
        
        return new EventGetResponseDto(findEvent);
    }
}
