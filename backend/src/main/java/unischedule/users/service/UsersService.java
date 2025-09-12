package unischedule.users.service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import unischedule.exception.InvalidInputException;
import unischedule.users.dto.EventCreateRequestDto;
import unischedule.users.dto.EventCreateResponseDto;
import unischedule.users.dto.EventGetResponseDto;
import unischedule.users.entity.Event;
import unischedule.users.repository.EventRepository;

@Service
@RequiredArgsConstructor
public class UsersService {
    private final EventRepository eventRepository;
    
    @Transactional
    public EventCreateResponseDto makeEvent(Long userId, EventCreateRequestDto requestDto) {
        
        boolean conflict = eventRepository.existsByCreatorIdAndStartAtLessThanAndEndAtGreaterThan(
            userId,
            requestDto.endTime(),
            requestDto.startTime()
        );
        
        if (conflict) {
            throw new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다.");
        }
        
        Event newEvent = Event.builder()
            .creatorId(userId)
            .title(requestDto.title())
            .content(requestDto.description())
            .startAt(requestDto.startTime())
            .endAt(requestDto.endTime())
            .state("CONFIRMED")
            .isPrivate(requestDto.isPrivate())
            .build();
        
        Event saved = eventRepository.save(newEvent);
        
        return new EventCreateResponseDto(saved);
    }
    
    public List<EventGetResponseDto> getEvents(LocalDateTime startAt, LocalDateTime endAt, Long userId) {
        List<Event> findEvents = eventRepository
            .findByCreatorIdAndStartAtLessThanAndEndAtGreaterThan(
                userId, endAt, startAt
            );
        
        return findEvents.stream().map(
            EventGetResponseDto::new
        ).toList();
    }
    
    //수정은 현재 테크 스펙 상 다른 도메인에 있어, 추후 추가 작성 필요
    //삭제는 현재 테크 스펙 상 없음
}
