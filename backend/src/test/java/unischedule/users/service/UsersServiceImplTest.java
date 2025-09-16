package unischedule.users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import unischedule.events.dto.EventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.entity.Event;
import unischedule.events.repository.EventRepository;
import unischedule.events.service.EventService;

@ExtendWith(MockitoExtension.class) // Mockito 초기화
class UsersServiceImplTest {
    @Mock
    private EventRepository eventRepository;
    
    @InjectMocks
    private EventService eventService;
    
    @Test
    void testMakeEvent() {
        // given
        
        EventCreateRequestDto requestDto = new EventCreateRequestDto(
            "회의", "주간 회의",
            LocalDateTime.now(), LocalDateTime.now().plusHours(1),
            true
        );
        
        Event savedEvent = new Event(
            "회의", "주간 회의",
            requestDto.startTime(), requestDto.endTime(),
            "CONFIRMED", true
        );
        
        Mockito.when(eventRepository.save(any(Event.class)))
            .thenReturn(savedEvent);
        
        Mockito.when(eventRepository.existsByStartAtLessThanAndEndAtGreaterThan(any(LocalDateTime.class), any(
                LocalDateTime.class)))
            .thenReturn(false);
        
        // when
        EventCreateResponseDto result = eventService.makeEvent(requestDto);
        
        // then
    }
    
    @Test
    void testGetEventsWithinRange() {
        // given
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 0, 0);
        LocalDateTime end   = LocalDateTime.of(2025, 9, 30, 23, 59);
        
        Event event1 = new Event(
            "회의", "주간 회의",
            LocalDateTime.of(2025, 9, 10, 10, 0),
            LocalDateTime.of(2025, 9, 10, 11, 0),
            "CONFIRMED", true
        );
        
        Event event2 = new Event("워크샵", "분기별 워크샵",
            LocalDateTime.of(2025, 9, 15, 14, 0),
            LocalDateTime.of(2025, 9, 15, 17, 0),
            "CONFIRMED", false
        );

        Mockito.when(eventRepository.findByStartAtLessThanAndEndAtGreaterThan(end, start))
            .thenReturn(List.of(event1, event2));
        
        // when
        List<EventGetResponseDto> result = eventService.getEvents(start, end);
        
        // then
        assertThat(result).hasSize(2);
    }
}
