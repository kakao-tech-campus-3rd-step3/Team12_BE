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

@ExtendWith(MockitoExtension.class) // Mockito 초기화
class UsersServiceImplTest {
    @Mock
    private EventRepository eventRepository;
    
    @InjectMocks
    private UsersService usersService;
    
    @Test
    void testMakeEvent() {
        // given
        
        EventCreateRequestDto requestDto = new EventCreateRequestDto(
            "회의", "주간 회의",
            LocalDateTime.now(), LocalDateTime.now().plusHours(1),
            true
        );
        
        Event savedEvent = new Event(
            1L, 1L, "회의", "주간 회의",
            requestDto.startTime(), requestDto.endTime(),
            "CONFIRMED", true
        );
        
        Mockito.when(eventRepository.save(any(Event.class)))
            .thenReturn(savedEvent);
        
        Mockito.when(eventRepository.existsByCreatorIdAndStartAtLessThanAndEndAtGreaterThan(eq(1L), any(LocalDateTime.class), any(
                LocalDateTime.class)))
            .thenReturn(false);
        
        // when
        EventCreateResponseDto result = usersService.makeEvent(1L, requestDto);
        
        // then
        assertThat(result.eventId()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("회의");
    }
    
    @Test
    void testGetEventsWithinRange() {
        // given
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 0, 0);
        LocalDateTime end   = LocalDateTime.of(2025, 9, 30, 23, 59);
        Long userId = 1L;
        
        Event event1 = new Event(
            1L, 1L, "회의", "주간 회의",
            LocalDateTime.of(2025, 9, 10, 10, 0),
            LocalDateTime.of(2025, 9, 10, 11, 0),
            "CONFIRMED", true
        );
        
        Event event2 = new Event(
            2L, 1L, "워크샵", "분기별 워크샵",
            LocalDateTime.of(2025, 9, 15, 14, 0),
            LocalDateTime.of(2025, 9, 15, 17, 0),
            "CONFIRMED", false
        );
        
        // Mockito로 findByStartAtGreaterThanEqualAndEndAtLessThanEqual 호출 시 반환값 지정
        Mockito.when(eventRepository.findByCreatorIdAndStartAtLessThanAndEndAtGreaterThan(userId, end, start))
            .thenReturn(List.of(event1, event2));
        
        // when
        List<EventGetResponseDto> result = usersService.getEvents(start, end, userId);
        
        // then
        assertThat(result).hasSize(2);
        
        assertThat(result.get(0).title()).isEqualTo("회의");
        assertThat(result.get(0).eventId()).isEqualTo(1L);
        
        assertThat(result.get(1).title()).isEqualTo("워크샵");
        assertThat(result.get(1).eventId()).isEqualTo(2L);
    }
}
