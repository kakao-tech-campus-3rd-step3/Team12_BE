package unischedule.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.events.dto.EventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.entity.Event;
import unischedule.events.entity.EventState;
import unischedule.events.repository.EventRepository;
import unischedule.events.service.EventService;
import unischedule.member.entity.Member;
import unischedule.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @Mock
    private EventRepository eventRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CalendarRepository calendarRepository;
    @InjectMocks
    private EventService eventService;
    
    @Test
    @DisplayName("특정 캘린더에 새 일정 등록")
    void makeEvent() {
        // given
        String userEmail = "test@example.com";
        Long memberId = 1L;
        Long calendarId = 1L;

        Member realOwner = new Member(userEmail, "testtest", "1q2w3e4r!");
        Member owner = Mockito.spy(realOwner);
        given(owner.getMemberId()).willReturn(memberId);

        Calendar realCalendar = new Calendar(owner, null, "test", "캘린더");
        Calendar calendar = Mockito.spy(realCalendar);

        EventCreateRequestDto requestDto = new EventCreateRequestDto(
            calendarId, "새 회의", "주간 회의",
            LocalDateTime.now(), LocalDateTime.now().plusHours(1),
            true
        );
        
        Event event = new Event(
            "새 회의", "주간 회의",
            requestDto.startTime(), requestDto.endTime(),
            EventState.CONFIRMED, true
        );

        given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(owner));
        given(calendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));
        given(eventRepository.existsScheduleInPeriod(eq(memberId), any(), any())).willReturn(false);
        given(eventRepository.save(any(Event.class))).willReturn(event);
        
        // when
        EventCreateResponseDto result = eventService.makeEvent(userEmail, requestDto);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("새 회의");
        assertThat(result.description()).isEqualTo("주간 회의");
    }
    
    @Test
    @DisplayName("사용자가 소유한 모든 캘린더 일정 기간 조회")
    void getMemberSchedule() {
        // given
        String userEmail = "test@gmail.com";
        Long memberId = 1L;
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 0, 0);
        LocalDateTime end   = LocalDateTime.of(2025, 9, 30, 23, 59);

        Member realMember = new Member(userEmail, "testtest", "1q2w3e4r!");
        Member member = Mockito.spy(realMember);
        given(member.getMemberId()).willReturn(memberId);

        Event event1 = new Event(
            "회의", "주간 회의",
            LocalDateTime.of(2025, 9, 10, 10, 0),
            LocalDateTime.of(2025, 9, 10, 11, 0),
            EventState.CONFIRMED, true
        );
        
        Event event2 = new Event("워크샵", "분기별 워크샵",
            LocalDateTime.of(2025, 9, 15, 14, 0),
            LocalDateTime.of(2025, 9, 15, 17, 0),
            EventState.CONFIRMED, false
        );


        given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(member));
        given(eventRepository.findScheduleInPeriod(eq(memberId), any(), any()))
                .willReturn(List.of(event1, event2));
        
        // when
        List<EventGetResponseDto> result = eventService.getEvents(userEmail, start, end);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo(event1.getTitle());
    }
}
