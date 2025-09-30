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
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarDomainService;
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.entity.Event;
import unischedule.events.entity.EventState;
import unischedule.events.service.PersonalEventService;
import unischedule.events.service.internal.EventDomainService;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.member.entity.Member;
import unischedule.member.service.internal.MemberDomainService;
import unischedule.util.TestUtil;

@ExtendWith(MockitoExtension.class)
class PersonalEventServiceTest {
    @Mock
    private EventDomainService eventDomainService;
    @Mock
    private MemberDomainService memberDomainService;
    @Mock
    private CalendarDomainService calendarDomainService;
    @InjectMocks
    private PersonalEventService eventService;

    private Member owner;
    private Calendar calendar;
    private String memberEmail;
    private Long calendarId;

    @BeforeEach
    void setUp() {
        memberEmail = "test@example.com";
        calendarId = 1L;

        owner = spy(TestUtil.makeMember());
        calendar = spy(TestUtil.makeCalendar(owner));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(memberDomainService, eventDomainService, calendarDomainService);
    }
    
    @Test
    @DisplayName("개인 캘린더에 새 일정 등록")
    void makeEvent() {
        // given
        PersonalEventCreateRequestDto requestDto = new PersonalEventCreateRequestDto(
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

        given(memberDomainService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(calendarDomainService.getMyPersonalCalendar(owner)).willReturn(calendar);

        doNothing().when(calendar).validateOwner(owner);
        doNothing().when(eventDomainService).validateNoSchedule(eq(owner), any(LocalDateTime.class), any(LocalDateTime.class));

        given(eventDomainService.saveEvent(any(Event.class))).willReturn(event);
        
        // when
        EventCreateResponseDto result = eventService.makePersonalEvent(memberEmail, requestDto);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("새 회의");
        assertThat(result.description()).isEqualTo("주간 회의");
        verify(eventDomainService).saveEvent(any(Event.class));
    }

    @Test
    @DisplayName("일정 등록 실패 - 시간이 겹칠 경우")
    void makeEventFailOnConflict() {
        // given
        PersonalEventCreateRequestDto requestDto = new PersonalEventCreateRequestDto(
                "겹치는 회의",
                "주간 회의",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                true
        );

        given(memberDomainService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(calendarDomainService.getMyPersonalCalendar(owner)).willReturn(calendar);

        doThrow(new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다."))
                .when(eventDomainService).validateNoSchedule(eq(owner), any(LocalDateTime.class), any(LocalDateTime.class));

        // when & then
        assertThatThrownBy(() -> eventService.makePersonalEvent(memberEmail, requestDto))
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


        given(memberDomainService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventDomainService.findSchedule(owner, start, end))
                .willReturn(List.of(event1, event2));
        
        // when
        List<EventGetResponseDto> result = eventService.getPersonalEvents(memberEmail, start, end);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().title()).isEqualTo(event1.getTitle());
    }

    @Test
    @DisplayName("일정 수정 성공 - 시간 변경 없음")
    void modifyEvent() {
        // given
        Long eventId = 10L;
        Event existingEvent = spy(TestUtil.makeEvent("일정", "내용"));

        existingEvent.connectCalendar(calendar);

        EventModifyRequestDto requestDto = new EventModifyRequestDto(eventId, "새 제목", "새 내용", null, null, true);

        given(memberDomainService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventDomainService.findEventById(eventId)).willReturn(existingEvent);
        doNothing().when(existingEvent).validateEventOwner(owner);

        // when
        EventGetResponseDto responseDto = eventService.modifyPersonalEvent(memberEmail, requestDto);

        // then
        assertThat(responseDto.title()).isEqualTo("새 제목");
        assertThat(responseDto.description()).isEqualTo("새 내용");
        assertThat(responseDto.isPrivate()).isTrue();

        verify(eventDomainService, never()).canUpdateEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("일정 수정 실패 - 다른 사용자의 일정")
    void modifyEventFailAccessDenied() {
        // given
        Long eventId = 10L;

        Event existingEvent = spy(TestUtil.makeEvent("다른 사람 일정", "내용"));

        existingEvent.connectCalendar(calendar);

        given(memberDomainService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventDomainService.findEventById(eventId)).willReturn(existingEvent);

        EventModifyRequestDto requestDto = new EventModifyRequestDto(10L, "새 제목", null, null, null, null);

        doThrow(new AccessDeniedException("해당 캘린더에 대한 접근 권한이 없습니다."))
                .when(existingEvent).validateEventOwner(any(Member.class));


        // when & then
        assertThatThrownBy(() -> eventService.modifyPersonalEvent(memberEmail, requestDto))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("해당 캘린더에 대한 접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("일정 삭제 성공")
    void deleteEvent() {
        // given
        Long eventId = 10L;
        Event eventToDelete = spy(TestUtil.makeEvent("일정", "내용"));
        eventToDelete.connectCalendar(calendar);

        given(memberDomainService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventDomainService.findEventById(eventId)).willReturn(eventToDelete);

        doNothing().when(eventToDelete).validateEventOwner(owner);
        doNothing().when(eventDomainService).deleteEvent(eventToDelete);

        // when
        eventService.deletePersonalEvent(memberEmail, eventId);

        // then
        verify(eventDomainService).deleteEvent(eventToDelete);
    }

    @Test
    @DisplayName("일정 삭제 실패 - 존재하지 않는 일정")
    void deleteEventFailNotFound() {
        // given
        Long eventId = 99L;
        given(memberDomainService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventDomainService.findEventById(eventId))
                .willThrow(new EntityNotFoundException("해당 일정을 찾을 수 없습니다."));

        // when & then
        assertThatThrownBy(() -> eventService.deletePersonalEvent(memberEmail, eventId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("해당 일정을 찾을 수 없습니다.");
        verify(eventDomainService, never()).deleteEvent(any());
    }
}
