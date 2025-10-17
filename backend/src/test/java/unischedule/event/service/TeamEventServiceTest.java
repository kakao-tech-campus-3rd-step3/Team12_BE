package unischedule.event.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventOverrideDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.EventUpdateDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.service.EventQueryService;
import unischedule.events.service.TeamEventService;
import unischedule.events.service.internal.EventOverrideRawService;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.service.internal.RecurringEventRawService;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;
import unischedule.util.TestUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TeamEventServiceTest {

    @Mock
    private MemberRawService memberRawService;
    @Mock
    private EventRawService eventRawService;
    @Mock
    private CalendarRawService calendarRawService;
    @Mock
    private TeamRawService teamRawService;
    @Mock
    private TeamMemberRawService teamMemberRawService;
    @Mock
    private EventQueryService eventQueryService;
    @Mock
    private EventOverrideRawService eventOverrideRawService;
    @Mock
    private RecurringEventRawService recurringEventRawService;
    @InjectMocks
    private TeamEventService teamEventService;

    @Test
    @DisplayName("팀 단일 일정 생성")
    void createTeamEvent() {
        // given
        String email = "test@example.com";
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Calendar teamCalendar = TestUtil.makeTeamCalendar(member, team);
        TeamEventCreateRequestDto requestDto = new TeamEventCreateRequestDto(
                1L, "팀 회의", "주간 보고",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(teamRawService.findTeamById(requestDto.teamId())).willReturn(team);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);

        // getAllRelatedCalendarIds
        given(teamMemberRawService.findByTeam(team)).willReturn(List.of(new TeamMember(team, member, null)));
        given(calendarRawService.getMyPersonalCalendar(any())).willReturn(TestUtil.makePersonalCalendar(member));
        given(calendarRawService.getTeamCalendar(any())).willReturn(teamCalendar);

        // 중복 검사
        doNothing().when(eventQueryService).checkNewSingleEventOverlap(anyList(), any(), any());
        given(eventRawService.saveEvent(any(Event.class))).willAnswer(invocation -> invocation.getArgument(0));


        // when
        EventCreateResponseDto responseDto = teamEventService.createTeamSingleEvent(email, requestDto);

        // then
        verify(eventQueryService).checkNewSingleEventOverlap(anyList(), eq(requestDto.startTime()), eq(requestDto.endTime()));
        verify(eventRawService).saveEvent(any(Event.class));
        assertThat(responseDto.title()).isEqualTo("팀 회의");
        assertThat(responseDto.description()).isEqualTo("주간 보고");
    }

    @Test
    @DisplayName("팀 일정 반복 생성")
    void createTeamRecurringEvent() {
        String email = "test@test.com";
        Long teamId = 1L;
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Calendar teamCalendar = TestUtil.makeTeamCalendar(member, team);

        RecurringEventCreateRequestDto requestDto = new RecurringEventCreateRequestDto(
                "팀 반복 회의",
                "매주 진행",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                false,
                "FREQ=WEEKLY;INTERVAL=1"
        );

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(teamRawService.findTeamById(teamId)).willReturn(team);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        given(calendarRawService.getTeamCalendar(team)).willReturn(teamCalendar);

        // getAllRelatedCalendarIds mocking
        given(teamMemberRawService.findByTeam(team)).willReturn(List.of(new TeamMember(team, member, null)));
        given(calendarRawService.getMyPersonalCalendar(any())).willReturn(TestUtil.makePersonalCalendar(member));

        doNothing().when(eventQueryService).checkNewRecurringEventOverlap(anyList(), any(), any(), any());
        given(eventRawService.saveEvent(any(Event.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        EventCreateResponseDto result = teamEventService.createTeamRecurringEvent(email, teamId, requestDto);

        // then
        verify(eventQueryService).checkNewRecurringEventOverlap(anyList(), eq(requestDto.firstStartTime()), eq(requestDto.firstEndTime()), eq(requestDto.rrule()));
        verify(eventRawService).saveEvent(any(Event.class));
        assertThat(result.title()).isEqualTo("팀 반복 회의");
    }

    @Test
    @DisplayName("팀 반복 일정 단건 수정")
    void modifyTeamRecurringInstance() {
        // given
        String email = "test@test.com";
        Long eventId = 1L;
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Event originalEvent = TestUtil.makeRecurringEvent("팀 반복", "");
        originalEvent.connectCalendar(TestUtil.makeTeamCalendar(member, team));
        LocalDateTime originalStartTime = originalEvent.getStartAt().plusWeeks(1);

        RecurringInstanceModifyRequestDto requestDto = new RecurringInstanceModifyRequestDto(
                originalStartTime,
                "수정된 팀 회의",
                null,
                null,
                null,
                null
        );

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(eventRawService.findEventById(eventId)).willReturn(originalEvent);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        given(eventOverrideRawService.findEventOverride(originalEvent, originalStartTime)).willReturn(Optional.empty());
        given(eventOverrideRawService.saveEventOverride(any(EventOverride.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        EventGetResponseDto result = teamEventService.modifyTeamRecurringInstance(email, eventId, requestDto);

        // then
        verify(eventOverrideRawService).saveEventOverride(any(EventOverride.class));
        assertThat(result.title()).isEqualTo("수정된 팀 회의");
        assertThat(result.isRecurring()).isTrue();
    }

    @Test
    @DisplayName("팀 반복 일정 단건 재수정")
    void modifyTeamRecurringInstance_Update() {
        String email = "test@test.com";
        Long eventId = 1L;
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Event originalEvent = TestUtil.makeRecurringEvent("팀 반복", "");
        originalEvent.connectCalendar(TestUtil.makeTeamCalendar(member, team));
        LocalDateTime originalStartTime = originalEvent.getStartAt().plusWeeks(1);

        EventOverride exsitingException = spy(EventOverride.makeEventOverride(
                originalEvent,
                new EventOverrideDto(
                        originalStartTime,
                        "첫 번째 수정",
                        null,
                        null,
                        null,
                        null
                )
        ));

        RecurringInstanceModifyRequestDto requestDto = new RecurringInstanceModifyRequestDto(
                originalStartTime,
                "두 번째 수정",
                null,
                null,
                null,
                null
        );

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(eventRawService.findEventById(eventId)).willReturn(originalEvent);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        given(eventOverrideRawService.findEventOverride(originalEvent, originalStartTime)).willReturn(Optional.of(exsitingException));
        given(eventOverrideRawService.saveEventOverride(any(EventOverride.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        teamEventService.modifyTeamRecurringInstance(email, eventId, requestDto);

        // then
        verify(eventOverrideRawService).updateEventOverride(any(EventOverride.class), any(EventOverrideDto.class));
        verify(eventOverrideRawService).saveEventOverride(exsitingException);
    }

    @Test
    @DisplayName("팀 일정 생성 실패 - 멤버 시간 중복")
    void createTeamEventFail() {
        // given
        String email = "test@example.com";
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        TeamEventCreateRequestDto requestDto = new TeamEventCreateRequestDto(
                1L, "팀 회의", "주간 보고",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), false);

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(teamRawService.findTeamById(requestDto.teamId())).willReturn(team);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        given(teamMemberRawService.findByTeam(team)).willReturn(List.of(new TeamMember(team, member, null)));
        given(calendarRawService.getMyPersonalCalendar(any())).willReturn(TestUtil.makePersonalCalendar(member));

        doThrow(new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다."))
                .when(eventQueryService).checkNewSingleEventOverlap(anyList(), any(), any());

        // when & then
        assertThatThrownBy(() -> teamEventService.createTeamSingleEvent(email, requestDto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("겹치는 일정이 있어 등록할 수 없습니다.");
    }

    @Test
    @DisplayName("팀 일정 수정")
    void modifyTeamEvent() {
        // given
        String email = "test@example.com";
        Long eventId = 1L;
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Calendar teamCalendar = spy(TestUtil.makeTeamCalendar(member, team));
        Event event = spy(TestUtil.makeEvent("Original Title", "Original Content"));
        event.connectCalendar(teamCalendar);

        EventModifyRequestDto requestDto = new EventModifyRequestDto(
                "Updated Title", "Updated Content", event.getStartAt(), event.getEndAt(), true);

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(eventRawService.findEventById(eventId)).willReturn(event);
        given(teamCalendar.getTeam()).willReturn(team);
        doNothing().when(event).validateIsTeamEvent();
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);

        // getAllRelatedCalendarIds mocking
        given(teamMemberRawService.findByTeam(team)).willReturn(List.of(new TeamMember(team, member, TeamRole.MEMBER)));
        given(calendarRawService.getMyPersonalCalendar(any())).willReturn(TestUtil.makePersonalCalendar(member));

        doAnswer(invocation -> {
            Event eventToModify = invocation.getArgument(0);
            EventUpdateDto updateDto = invocation.getArgument(1);
            eventToModify.modifyEvent(
                    updateDto.title(), updateDto.content(), updateDto.startTime(),
                    updateDto.endTime(), updateDto.isPrivate());
            return null;
        }).when(eventRawService).updateEvent(any(Event.class), any(EventUpdateDto.class));

        // when
        EventGetResponseDto responseDto = teamEventService.modifyTeamEvent(email, eventId, requestDto);

        // then
        assertThat(responseDto.title()).isEqualTo("Updated Title");
        assertThat(responseDto.description()).isEqualTo("Updated Content");
        assertThat(responseDto.isPrivate()).isTrue();
        verify(eventRawService).updateEvent(any(Event.class), any());
    }

    @Test
    @DisplayName("팀 일정 삭제")
    void deleteTeamEvent() {
        // given
        String email = "test@example.com";
        Long eventId = 1L;
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Calendar teamCalendar = spy(TestUtil.makeTeamCalendar(member, team));
        Event event = spy(TestUtil.makeEvent("Event to Delete", "Content"));
        event.connectCalendar(teamCalendar);

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(eventRawService.findEventById(eventId)).willReturn(event);
        given(teamCalendar.getTeam()).willReturn(team);
        doNothing().when(event).validateIsTeamEvent();
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);

        // when
        teamEventService.deleteTeamEvent(email, eventId);

        // then
        verify(eventRawService).deleteEvent(event);
    }
}
