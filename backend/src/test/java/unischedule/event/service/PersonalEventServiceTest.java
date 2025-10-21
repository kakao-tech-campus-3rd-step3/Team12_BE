package unischedule.event.service;

import java.time.LocalDate;
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
import unischedule.events.domain.EventOverride;
import unischedule.events.domain.EventState;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventOverrideDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.dto.EventUpdateDto;
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.service.EventQueryService;
import unischedule.events.service.PersonalEventService;
import unischedule.events.service.internal.EventOverrideRawService;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.service.internal.RecurringEventRawService;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.util.TestUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    private RecurringEventRawService recurringEventRawService;
    @Mock
    private EventQueryService eventQueryService;
    @Mock
    private CalendarRawService calendarRawService;
    @Mock
    private TeamMemberRawService teamMemberRawService;
    @Mock
    private EventOverrideRawService eventOverrideRawService;
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
        given(personalCalendar.getCalendarId()).willReturn(calendarId);

        doNothing().when(personalCalendar).validateOwner(owner);
        doNothing().when(eventQueryService).checkNewSingleEventOverlap(anyList(), any(LocalDateTime.class), any(LocalDateTime.class));

        given(eventRawService.saveEvent(any(Event.class))).willReturn(event);
        
        // when
        EventCreateResponseDto result = eventService.makePersonalSingleEvent(memberEmail, requestDto);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("새 회의");
        assertThat(result.description()).isEqualTo("주간 회의");
        verify(eventQueryService).checkNewSingleEventOverlap(eq(List.of(calendarId)), eq(requestDto.startTime()), eq(requestDto.endTime()));
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
        given(personalCalendar.getCalendarId()).willReturn(calendarId);

        doThrow(new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다."))
                .when(eventQueryService).checkNewSingleEventOverlap(anyList(), any(LocalDateTime.class), any(LocalDateTime.class));

        // when & then
        assertThatThrownBy(() -> eventService.makePersonalSingleEvent(memberEmail, requestDto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("겹치는 일정이 있어 등록할 수 없습니다.");

        verify(eventQueryService).checkNewSingleEventOverlap(eq(List.of(calendarId)), eq(requestDto.startTime()), eq(requestDto.endTime()));
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

        EventServiceDto event1 = new EventServiceDto(
                1L,
                "회의",
                "주간 회의",
                LocalDateTime.of(2025, 9, 10, 10, 0),
                LocalDateTime.of(2025, 9, 10, 11, 0),
                EventState.CONFIRMED,
                true,
                false
        );

        EventServiceDto event2 = new EventServiceDto(
                2L,
                "워크샵",
                "분기별 워크샵",
                LocalDateTime.of(2025, 9, 15, 14, 0),
                LocalDateTime.of(2025, 9, 15, 17, 0),
                EventState.CONFIRMED,
                false,
                false
        );

        List<Long> calendarIds = List.of(1L, 2L);

        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(calendarRawService.getMyPersonalCalendar(owner)).willReturn(personalCalendar);
        given(teamMemberRawService.findByMember(owner)).willReturn(List.of(teamMember));
        given(calendarRawService.getTeamCalendar(team)).willReturn(teamCalendar);

        given(eventQueryService.getEvents(calendarIds, start, end))
                .willReturn(List.of(event1, event2));

        // when
        List<EventGetResponseDto> result = eventService.getPersonalEvents(memberEmail, start, end);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.getFirst().title()).isEqualTo(event1.title());
        assertThat(result.get(1).title()).isEqualTo(event2.title());

        verify(eventQueryService).getEvents(calendarIds, start, end);
    }

    @Test
    @DisplayName("일정 수정 성공 - 시간 변경 없음")
    void modifyEvent() {
        // given
        Long eventId = 10L;
        Event existingEvent = TestUtil.makeEvent("일정", "내용");

        existingEvent.connectCalendar(personalCalendar);

        EventModifyRequestDto requestDto = new EventModifyRequestDto("새 제목", "새 내용", null, null, true);

        given(personalCalendar.getCalendarId()).willReturn(1L);
        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventRawService.findEventById(eventId)).willReturn(existingEvent);

        doNothing().when(eventQueryService).checkEventUpdateOverlap(anyList(), any(), any(), any());

        // when
        eventService.modifyPersonalEvent(memberEmail, eventId, requestDto);

        // then
        verify(eventRawService).updateEvent(eq(existingEvent), any(EventUpdateDto.class));
    }

    @Test
    @DisplayName("반복 일정 날짜 최초 수정 시 EventOverride 생성")
    void modifyRecurringInstance_CreateNewEventOverride() {
        // given
        Long eventId = 1L;
        Event originalEvent = TestUtil.makeRecurringEvent("반복 회의", "주간 회의");
        originalEvent.connectCalendar(personalCalendar);
        LocalDateTime originalStartTime = originalEvent.getStartAt().plusDays(7);

        RecurringInstanceModifyRequestDto requestDto = new RecurringInstanceModifyRequestDto(
                originalStartTime,
                "변경된 회의",
                "내용 변경",
                originalStartTime.plusHours(1),
                originalStartTime.plusHours(2),
                false
        );

        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventRawService.findEventById(eventId)).willReturn(originalEvent);
        given(eventOverrideRawService.findEventOverride(originalEvent, requestDto.originalStartTime()))
                .willReturn(Optional.empty());
        given(eventOverrideRawService.saveEventOverride(any(EventOverride.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        EventGetResponseDto result = eventService.modifyPersonalRecurringInstance(memberEmail, eventId, requestDto);

        // then
        verify(eventOverrideRawService).saveEventOverride(any(EventOverride.class));
        assertThat(result.eventId()).isEqualTo(originalEvent.getEventId());
        assertThat(result.title()).isEqualTo("변경된 회의");
        assertThat(result.description()).isEqualTo("내용 변경");
    }

    @Test
    @DisplayName("반복 일정의 특정 날짜(instance) 재수정 시 기존 EventOverride 업데이트")
    void modifyRecurringInstance_UpdateExistingOverride() {
        // given
        Long eventId = 1L;
        Event originalEvent = TestUtil.makeRecurringEvent("반복 일정", "주간 회의");
        originalEvent.connectCalendar(personalCalendar);
        LocalDateTime originalStartTime = originalEvent.getStartAt().plusDays(7);

        EventOverride existingOverride = spy(EventOverride.makeEventOverride(
                originalEvent,
                new EventOverrideDto(originalStartTime, "첫 번째 수정", null, null, null, null)
        ));

        RecurringInstanceModifyRequestDto requestDto = new RecurringInstanceModifyRequestDto(
                originalStartTime,
                "두 번째 수정",
                "내용도 수정",
                null,
                null,
                null
        );

        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventRawService.findEventById(eventId)).willReturn(originalEvent);
        given(eventOverrideRawService.findEventOverride(originalEvent, requestDto.originalStartTime()))
                .willReturn(Optional.of(existingOverride));
        given(eventOverrideRawService.saveEventOverride(any(EventOverride.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        doAnswer(invocation -> {
            EventOverride exception = invocation.getArgument(0);
            EventOverrideDto dto = invocation.getArgument(1);
            exception.update(dto.originalStartTime(), dto.title(), dto.content(), dto.startTime(), dto.endTime(), dto.isPrivate());
            return null;
        }).when(eventOverrideRawService).updateEventOverride(any(EventOverride.class), any(EventOverrideDto.class));

        // when
        EventGetResponseDto result = eventService.modifyPersonalRecurringInstance(memberEmail, eventId, requestDto);

        // then
        verify(eventOverrideRawService).updateEventOverride(any(EventOverride.class), any(EventOverrideDto.class));
        verify(eventOverrideRawService).saveEventOverride(existingOverride);
        assertThat(result.title()).isEqualTo("두 번째 수정");
        assertThat(result.description()).isEqualTo("내용도 수정");
    }

    @Test
    @DisplayName("일정 수정 실패 - 다른 사용자의 일정")
    void modifyEventFailAccessDenied() {
        // given
        Long eventId = 10L;

        Event existingEvent = spy(TestUtil.makeEvent("다른 사람 일정", "내용"));

        existingEvent.connectCalendar(personalCalendar);
        EventModifyRequestDto requestDto = new EventModifyRequestDto("새 제목", null, null, null, null);

        given(memberRawService.findMemberByEmail(memberEmail)).willReturn(owner);
        given(eventRawService.findEventById(eventId)).willReturn(existingEvent);

        doThrow(new AccessDeniedException("해당 캘린더에 대한 접근 권한이 없습니다."))
                .when(existingEvent).validateEventOwner(any(Member.class));


        // when & then
        assertThatThrownBy(() -> eventService.modifyPersonalEvent(memberEmail, eventId, requestDto))
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
        //given
        String email = "test@test.com";
        
        Member member = owner;
        Team team = TestUtil.makeTeam();
        TeamMember teamMember = new TeamMember(team, member, TeamRole.LEADER);
        
        Calendar teamCalendar = spy(TestUtil.makeTeamCalendar(owner, team));
        when(teamCalendar.getCalendarId()).thenReturn(100L);
        
        Calendar personalCalendar = spy(TestUtil.makePersonalCalendar(owner));
        when(personalCalendar.getCalendarId()).thenReturn(200L);
        
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        when(teamMemberRawService.findByMember(member)).thenReturn(List.of(teamMember));
        when(calendarRawService.getTeamCalendar(team)).thenReturn(teamCalendar);
        when(calendarRawService.getMyPersonalCalendar(member)).thenReturn(personalCalendar);
        
        LocalDateTime start = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(8).atStartOfDay();
        
        EventServiceDto event1 = new EventServiceDto(
            1L,
            "회의",
            "주간 회의",
            LocalDateTime.of(2025, 9, 10, 10, 0),
            LocalDateTime.of(2025, 9, 10, 11, 0),
            EventState.CONFIRMED,
            true,
            false
        );
        
        EventServiceDto event2 = new EventServiceDto(
            2L,
            "워크샵",
            "분기별 워크샵",
            LocalDateTime.of(2025, 9, 15, 14, 0),
            LocalDateTime.of(2025, 9, 15, 17, 0),
            EventState.CONFIRMED,
            false,
            false
        );
        
        when(eventQueryService.getEvents(anyList(), eq(start), eq(end)))
            .thenReturn(List.of(event1, event2));
        
        // when
        List<EventGetResponseDto> result = eventService.getUpcomingMyEvent(email);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("회의");
        
        verify(memberRawService).findMemberByEmail(email);
        verify(teamMemberRawService).findByMember(member);
        verify(calendarRawService).getTeamCalendar(team);
        verify(calendarRawService).getMyPersonalCalendar(member);
        verify(eventQueryService).getEvents(anyList(), eq(start), eq(end));
    }
    
    @Test
    @DisplayName("오늘의 개인 일정 조회 성공")
    void getTodayMyEvent_success() {
        //given
        String email = "today@test.com";
        
        Member member = owner;
        Team team = TestUtil.makeTeam();
        TeamMember teamMember = TestUtil.makeTeamMember(team, member);
        
        Calendar teamCalendar = spy(TestUtil.makeTeamCalendar(owner, team));
        when(teamCalendar.getCalendarId()).thenReturn(100L);
        
        Calendar personalCalendar = spy(TestUtil.makePersonalCalendar(owner));
        when(personalCalendar.getCalendarId()).thenReturn(200L);
        
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        
        EventServiceDto event1 = new EventServiceDto(
            1L,
            "회의",
            "주간 회의",
            LocalDateTime.of(2025, 9, 10, 10, 0),
            LocalDateTime.of(2025, 9, 10, 11, 0),
            EventState.CONFIRMED,
            true,
            false
        );
        
        EventServiceDto event2 = new EventServiceDto(
            2L,
            "워크샵",
            "분기별 워크샵",
            LocalDateTime.of(2025, 9, 15, 14, 0),
            LocalDateTime.of(2025, 9, 15, 17, 0),
            EventState.CONFIRMED,
            false,
            false
        );
        
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        when(teamMemberRawService.findByMember(member)).thenReturn(List.of(teamMember));
        when(calendarRawService.getTeamCalendar(team)).thenReturn(teamCalendar);
        when(calendarRawService.getMyPersonalCalendar(member)).thenReturn(personalCalendar);
        
        when(eventQueryService.getEvents(anyList(), eq(start), eq(end))).thenReturn(List.of(event1, event2));
        
        //when
        List<EventGetResponseDto> result = eventService.getTodayMyEvent(email);
        
        //then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("회의");
        assertThat(result.get(1).title()).isEqualTo("워크샵");
        
        verify(memberRawService).findMemberByEmail(email);
        verify(teamMemberRawService).findByMember(member);
        verify(calendarRawService).getTeamCalendar(team);
        verify(calendarRawService).getMyPersonalCalendar(member);
        verify(eventQueryService).getEvents(anyList(), eq(start), eq(end));
    }
}
