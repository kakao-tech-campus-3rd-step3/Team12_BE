package unischedule.events.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.exception.EntityNotFoundException;
import unischedule.users.dto.EventGetResponseDto;
import unischedule.users.entity.Event;
import unischedule.users.repository.EventRepository;

@ExtendWith(MockitoExtension.class)
class EventsServiceImplTest {
    @Mock
    private EventRepository eventRepository;
    
    @InjectMocks
    private EventsServiceImpl eventsService;
    
    @Test
    void modifyEvent_success() {
        // given
        Long eventId = 1L;
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 0, 0);
        LocalDateTime end   = LocalDateTime.of(2025, 9, 1, 3, 0);
        EventModifyRequestDto requestDto = new EventModifyRequestDto("새 제목", "새 내용", null, null, null);
        
        Event event = new Event(
            1L, 1L, "회의", "주간 회의",
            start, end, "CONFIRMED", true);
        given(eventRepository.findById(eventId)).willReturn(Optional.of(event));
        
        // when
        EventGetResponseDto response = eventsService.modifyEvent(eventId, requestDto);
        
        // then
        assertThat(response.title()).isEqualTo("새 제목");
        assertThat(response.description()).isEqualTo("새 내용");
    }
    
    @Test
    void modifyEvent_notFound_throwsException() {
        // given
        Long eventId = 99L;
        EventModifyRequestDto requestDto = new EventModifyRequestDto("제목", "내용", null, null, null);
        
        given(eventRepository.findById(eventId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> eventsService.modifyEvent(eventId, requestDto))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessage("해당 이벤트가 없습니다.");
    }
}