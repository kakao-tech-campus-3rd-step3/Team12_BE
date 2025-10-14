package unischedule.event.service;

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
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventState;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.EventUpdateDto;
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.repository.EventExceptionRepository;
import unischedule.events.service.PersonalEventService;
import unischedule.events.service.internal.EventRawService;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.util.TestUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalEventServiceTest {
    @Mock
    private EventRawService eventRawService;
    @Mock
    private MemberRawService memberRawService;
    @Mock
    private CalendarRawService calendarRawService;
    @Mock
    private TeamMemberRawService teamMemberRawService;
    @Mock
    private EventExceptionRepository eventExceptionRepository;
    @InjectMocks
    private PersonalEventService eventService;

    private Member owner;
    private Calendar personalCalendar;
    private String memberEmail;
    private Long calendarId;

    @BeforeEach
    void setUp() {
        memberEmail = "test@example.com";
        calendarId = 1L;

        owner = spy(TestUtil.makeMember());
        personalCalendar = spy(TestUtil.makePersonalCalendar(owner));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(memberRawService, eventRawService, calendarRawService, teamMemberRawService);
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

        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(calendarRawService.getMyPersonalCalendar(owner)).willReturn(personalCalendar);

        doNothing().when(personalCalendar).validateOwner(owner);
        doNothing().when(eventRawService).validateNoSingleSchedule(eq(owner), any(LocalDateTime.class), any(LocalDateTime.class));

        given(eventRawService.saveEvent(any(Event.class))).willReturn(event);
        
        // when
        EventCreateResponseDto result = eventService.makePersonalEvent(memberEmail, requestDto);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("새 회의");
        assertThat(result.description()).isEqualTo("주간 회의");
        verify(eventRawService).saveEvent(any(Event.class));
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

        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(calendarRawService.getMyPersonalCalendar(owner)).willReturn(personalCalendar);

        doThrow(new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다."))
                .when(eventRawService).validateNoSingleSchedule(eq(owner), any(LocalDateTime.class), any(LocalDateTime.class));

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

        Team team = TestUtil.makeTeam();
        TeamMember teamMember = TestUtil.makeTeamMember(team, owner);
        Calendar teamCalendar = mock(Calendar.class);
        given(teamCalendar.getCalendarId()).willReturn(2L);
        given(personalCalendar.getCalendarId()).willReturn(1L);

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

        List<Long> calendarIds = List.of(1L, 2L);

        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(calendarRawService.getMyPersonalCalendar(owner)).willReturn(personalCalendar);
        given(teamMemberRawService.findByMember(owner)).willReturn(List.of(teamMember));
        given(calendarRawService.getTeamCalendar(team)).willReturn(teamCalendar);

        given(eventRawService.findSingleSchedule(calendarIds, start, end))
                .willReturn(List.of(event1, event2));
        given(eventRawService.findRecurringSchedule(calendarIds, end))
                .willReturn(new ArrayList<>()); // 이 테스트에서는 반복 일정이 없다고 가정
        given(eventExceptionRepository.findEventExceptionsForEvents(any(), any(), any()))
                .willReturn(new ArrayList<>()); // 예외도 없다고 가정

        // when
        List<EventGetResponseDto> result = eventService.getPersonalEvents(memberEmail, start, end);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().title()).isEqualTo(event1.getTitle());
        assertThat(result.get(0).title()).isEqualTo(event1.getTitle());
        assertThat(result.get(1).title()).isEqualTo(event2.getTitle());

        verify(memberRawService).findMemberByEmail(memberEmail);
        verify(calendarRawService).getMyPersonalCalendar(owner);
        verify(teamMemberRawService).findByMember(owner);
        verify(calendarRawService).getTeamCalendar(team);

        verify(eventRawService).findSingleSchedule(calendarIds, start, end);
        verify(eventRawService).findRecurringSchedule(calendarIds, end);
        verify(eventExceptionRepository).findEventExceptionsForEvents(any(), any(), any());
    }

    @Test
    @DisplayName("일정 수정 성공 - 시간 변경 없음")
    void modifyEvent() {
        // given
        Long eventId = 10L;
        Event existingEvent = TestUtil.makeEvent("일정", "내용");

        existingEvent.connectCalendar(personalCalendar);

        EventModifyRequestDto requestDto = new EventModifyRequestDto(eventId, "새 제목", "새 내용", null, null, true);

        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventRawService.findEventById(eventId)).willReturn(existingEvent);

        doAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            EventUpdateDto dto = invocation.getArgument(1);
            event.modifyEvent(dto.title(), dto.content(), dto.startTime(), dto.endTime(), dto.isPrivate());
            return null;
        }).when(eventRawService).updateEvent(any(Event.class), any(EventUpdateDto.class));

        // when
        EventGetResponseDto responseDto = eventService.modifyPersonalEvent(memberEmail, requestDto);

        // then
        assertThat(responseDto.title()).isEqualTo("새 제목");
        assertThat(responseDto.description()).isEqualTo("새 내용");
        assertThat(responseDto.isPrivate()).isTrue();

        verify(eventRawService).canUpdateEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("일정 수정 실패 - 다른 사용자의 일정")
    void modifyEventFailAccessDenied() {
        // given
        Long eventId = 10L;

        Event existingEvent = spy(TestUtil.makeEvent("다른 사람 일정", "내용"));

        existingEvent.connectCalendar(personalCalendar);

        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventRawService.findEventById(eventId)).willReturn(existingEvent);

        EventModifyRequestDto requestDto = new EventModifyRequestDto(eventId, "새 제목", null, null, null, null);

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
        eventToDelete.connectCalendar(personalCalendar);

        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventRawService.findEventById(eventId)).willReturn(eventToDelete);

        doNothing().when(eventToDelete).validateEventOwner(owner);
        doNothing().when(eventRawService).deleteEvent(eventToDelete);

        // when
        eventService.deletePersonalEvent(memberEmail, eventId);

        // then
        verify(eventRawService).deleteEvent(eventToDelete);
    }

    @Test
    @DisplayName("일정 삭제 실패 - 존재하지 않는 일정")
    void deleteEventFailNotFound() {
        // given
        Long eventId = 99L;
        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventRawService.findEventById(eventId))
                .willThrow(new EntityNotFoundException("해당 일정을 찾을 수 없습니다."));

        // when & then
        assertThatThrownBy(() -> eventService.deletePersonalEvent(memberEmail, eventId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("해당 일정을 찾을 수 없습니다.");
        verify(eventRawService, never()).deleteEvent(any());
    }
    
    @Test
    @DisplayName("다가오는 개인 일정 조회 성공")
    void getUpcomingMyEvent_success() {
        // given
        String email = "test@test.com";
        Member member = new Member(email, "tester", "1234");
        Event event1 = Event.builder()
            .title("회의")
            .content("팀 회의")
            .startAt(LocalDateTime.now().plusHours(1))
            .endAt(LocalDateTime.now().plusHours(2))
            .state(EventState.CONFIRMED)
            .isPrivate(false)
            .build();
        
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        when(eventRawService.findUpcomingEventsByMember(member)).thenReturn(List.of(event1));
        
        // when
        List<EventGetResponseDto> result = eventService.getUpcomingMyEvent(email);
        
        // then
        verify(memberRawService, times(1)).findMemberByEmail(email);
        verify(eventRawService, times(1)).findUpcomingEventsByMember(member);
        
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("회의");
    }
    
    @Test
    @DisplayName("오늘의 개인 일정 조회 성공")
    void getTodayMyEvent_success() {
        // given
        String email = "today@test.com";
        Member member = new Member(email, "todayUser", "pw");
        Event event1 = Event.builder()
            .title("점심 회의")
            .content("오늘 점심 회의")
            .startAt(LocalDateTime.now().withHour(12))
            .endAt(LocalDateTime.now().withHour(13))
            .state(EventState.CONFIRMED)
            .isPrivate(true)
            .build();
        Event event2 = Event.builder()
            .title("스터디")
            .content("오늘 저녁 스터디")
            .startAt(LocalDateTime.now().withHour(19))
            .endAt(LocalDateTime.now().withHour(21))
            .state(EventState.CONFIRMED)
            .isPrivate(false)
            .build();
        
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        when(eventRawService.findTodayEventsByMember(member)).thenReturn(List.of(event1, event2));
        
        // when
        List<EventGetResponseDto> result = eventService.getTodayMyEvent(email);
        
        // then
        verify(memberRawService, times(1)).findMemberByEmail(email);
        verify(eventRawService, times(1)).findTodayEventsByMember(member);
        
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("점심 회의");
        assertThat(result.get(1).title()).isEqualTo("스터디");
    }
}
