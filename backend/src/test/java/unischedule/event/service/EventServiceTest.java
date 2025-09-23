package unischedule.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.events.dto.EventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.entity.Event;
import unischedule.events.entity.EventState;
import unischedule.events.repository.EventRepository;
import unischedule.events.service.EventService;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
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

    private Member owner;
    private Calendar calendar;
    private String userEmail = "test@example.com";
    private Long memberId = 1L;
    private Long calendarId = 1L;

    @BeforeEach
    void setUp() {
        Member realOwner = new Member(userEmail, "testtest", "1q2w3e4r!");
        owner = spy(realOwner);

        Calendar realCalendar = new Calendar(owner, "My Calendar", "Personal calendar");
        calendar = spy(realCalendar);
    }
    
    @Test
    @DisplayName("특정 캘린더에 새 일정 등록")
    void makeEvent() {
        // given
        EventCreateRequestDto requestDto = new EventCreateRequestDto(
                calendarId,
                "새 회의",
                "주간 회의",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                true
        );
        
        Event event = new Event(
                "새 회의",
                "주간 회의",
                requestDto.startTime(),
                requestDto.endTime(),
                EventState.CONFIRMED,
                true
        );

        given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(owner));
        given(calendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));
        given(eventRepository.existsPersonalScheduleInPeriod(eq(memberId), any(), any())).willReturn(false);
        given(eventRepository.save(any(Event.class))).willReturn(event);

        given(owner.getMemberId()).willReturn(memberId);
        
        // when
        EventCreateResponseDto result = eventService.makePersonalEvent(userEmail, requestDto);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("새 회의");
        assertThat(result.description()).isEqualTo("주간 회의");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("일정 등록 실패 - 시간이 겹칠 경우")
    void makeEventFailOnConflict() {
        // given
        EventCreateRequestDto requestDto = new EventCreateRequestDto(
                calendarId,
                "겹치는 회의",
                "주간 회의",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                true
        );

        given(owner.getMemberId()).willReturn(memberId);
        given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(owner));
        given(calendarRepository.findById(calendarId)).willReturn(Optional.of(calendar));
        given(eventRepository.existsPersonalScheduleInPeriod(eq(memberId), any(), any())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> eventService.makePersonalEvent(userEmail, requestDto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("겹치는 일정이 있어 등록할 수 없습니다.");
    }

    @Test
    @DisplayName("사용자가 소유한 모든 캘린더 일정 기간 조회")
    void getMemberSchedule() {
        // given
        LocalDateTime start = LocalDateTime.of(2025, 9, 1, 0, 0);
        LocalDateTime end   = LocalDateTime.of(2025, 9, 30, 23, 59);

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

        given(owner.getMemberId()).willReturn(memberId);


        given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(owner));
        given(eventRepository.findPersonalScheduleInPeriod(eq(memberId), any(), any()))
                .willReturn(List.of(event1, event2));
        
        // when
        List<EventGetResponseDto> result = eventService.getPersonalEvents(userEmail, start, end);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo(event1.getTitle());
    }

    @Test
    @DisplayName("일정 수정 성공")
    void modifyEvent() {
        // given
        Long eventId = 10L;
        Event existingEvent = new Event(
                "기존 제목",
                "기존 내용",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                EventState.CONFIRMED,
                false
        );

        existingEvent.connectCalendar(calendar);

        EventModifyRequestDto requestDto = new EventModifyRequestDto(10L, "새 제목", "새 내용", null, null, true);

        given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(owner));
        given(eventRepository.findById(eventId)).willReturn(Optional.of(existingEvent));

        // when
        EventGetResponseDto responseDto = eventService.modifyPersonalEvent(userEmail, requestDto);

        // then
        assertThat(responseDto.title()).isEqualTo("새 제목");
        assertThat(responseDto.description()).isEqualTo("새 내용");
        assertThat(responseDto.isPrivate()).isTrue();
    }

    @Test
    @DisplayName("일정 수정 실패 - 다른 사용자의 일정")
    void modifyEventFailAccessDenied() {
        // given
        Long eventId = 10L;
        Member anotherMember = spy(new Member("another@example.com", "another", "pw"));
        calendar.getOwner();

        Event existingEvent = new Event(
                "기존 제목",
                "기존 내용",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                EventState.CONFIRMED,
                false
        );
        existingEvent.connectCalendar(calendar);

        doThrow(new AccessDeniedException("해당 캘린더에 대한 접근 권한이 없습니다."))
                .when(calendar).validateOwner(any(Member.class));
        EventModifyRequestDto requestDto = new EventModifyRequestDto(10L, "새 제목", null, null, null, null);

        given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(owner));
        given(eventRepository.findById(eventId)).willReturn(Optional.of(existingEvent));

        // when & then
        assertThatThrownBy(() -> eventService.modifyPersonalEvent(userEmail, requestDto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 캘린더에 대한 접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("일정 삭제 성공")
    void deleteEvent() {
        // given
        Long eventId = 10L;
        Event eventToDelete = new Event(
                "기존 제목",
                "기존 내용",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                EventState.CONFIRMED,
                false
        );
        eventToDelete.connectCalendar(calendar);

        given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(owner));
        given(eventRepository.findById(eventId)).willReturn(Optional.of(eventToDelete));
        doNothing().when(eventRepository).delete(eventToDelete);

        // when
        eventService.deletePersonalEvent(userEmail, eventId);

        // then
        verify(eventRepository).delete(eventToDelete);
    }

    @Test
    @DisplayName("일정 삭제 실패 - 존재하지 않는 일정")
    void deleteEventFailNotFound() {
        // given
        Long eventId = 99L;
        given(memberRepository.findByEmail(userEmail)).willReturn(Optional.of(owner));
        given(eventRepository.findById(eventId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventService.deletePersonalEvent(userEmail, eventId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("해당 일정을 찾을 수 없습니다.");
        verify(eventRepository, never()).delete(any());
    }
}
