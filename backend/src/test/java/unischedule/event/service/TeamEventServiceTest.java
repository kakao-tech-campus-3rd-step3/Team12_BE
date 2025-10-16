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
import unischedule.events.domain.EventState;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.EventUpdateDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.service.TeamEventService;
import unischedule.events.service.internal.EventRawService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @InjectMocks
    private TeamEventService teamEventService;

    @Test
    @DisplayName("팀 일정 생성")
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
        given(teamMemberRawService.findByTeam(team)).willReturn(List.of(new TeamMember(team, member, null)));
        doNothing().when(eventRawService).validateNoScheduleForMembers(anyList(), any(), any());
        given(calendarRawService.getTeamCalendar(team)).willReturn(teamCalendar);
        given(eventRawService.saveEvent(any(Event.class))).willAnswer(invocation -> invocation.getArgument(0));


        // when
        EventCreateResponseDto responseDto = teamEventService.createTeamEvent(email, requestDto);

        // then
        assertThat(responseDto.title()).isEqualTo("팀 회의");
        assertThat(responseDto.description()).isEqualTo("주간 보고");
        verify(eventRawService).saveEvent(any(Event.class));
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
        doThrow(new InvalidInputException("일정이 겹치는 멤버가 있습니다."))
                .when(eventRawService).validateNoScheduleForMembers(anyList(), any(), any());

        // when & then
        assertThatThrownBy(() -> teamEventService.createTeamEvent(email, requestDto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("일정이 겹치는 멤버가 있습니다.");
    }


    @Test
    @DisplayName("팀 일정 수정")
    void modifyTeamEvent() {
        // given
        String email = "test@example.com";
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Calendar teamCalendar = spy(TestUtil.makeTeamCalendar(member, team));
        Event event = spy(TestUtil.makeEvent("Original Title", "Original Content"));
        event.connectCalendar(teamCalendar);

        EventModifyRequestDto requestDto = new EventModifyRequestDto(1L, "Updated Title", "Updated Content", null, null, true);

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(eventRawService.findEventById(requestDto.eventId())).willReturn(event);
        given(teamCalendar.getTeam()).willReturn(team);
        doNothing().when(event).validateIsTeamEvent();
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        given(teamMemberRawService.findByTeam(team)).willReturn(List.of(new TeamMember(team, member, TeamRole.MEMBER)));

        doAnswer(invocation -> {
            Event eventToModify = invocation.getArgument(0);
            EventUpdateDto updateDto = invocation.getArgument(1);
            eventToModify.modifyEvent(
                    updateDto.title(),
                    updateDto.content(),
                    updateDto.startTime(),
                    updateDto.endTime(),
                    updateDto.isPrivate()
            );
            return null;
        }).when(eventRawService).updateEvent(any(Event.class), any(EventUpdateDto.class));

        // when
        EventGetResponseDto responseDto = teamEventService.modifyTeamEvent(email, requestDto);

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
    
    @Test
    @DisplayName("다가오는 팀 일정 조회 성공")
    void getUpcomingTeamEvents_success() {
        // given
        String email = "team@test.com";
        Long teamId = 1L;
        
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        
        Calendar teamCalendar = spy(TestUtil.makeTeamCalendar(member, team));
        when(teamCalendar.getCalendarId()).thenReturn(100L);
        
        Event event1 = Event.builder()
            .title("팀 회의")
            .content("이번 주 팀 회의")
            .startAt(LocalDateTime.now().plusHours(1))
            .endAt(LocalDateTime.now().plusHours(2))
            .state(EventState.CONFIRMED)
            .isPrivate(false)
            .build();
        
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        when(teamRawService.findTeamById(teamId)).thenReturn(team);
        when(calendarRawService.getTeamCalendar(team)).thenReturn(teamCalendar);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        when(eventRawService.findUpcomingEventsByCalendar(anyList())).thenReturn(List.of(event1));
        
        // when
        List<EventGetResponseDto> result = teamEventService.getUpcomingTeamEvents(email, teamId);
        
        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("팀 회의");
        
        verify(memberRawService).findMemberByEmail(email);
        verify(teamRawService).findTeamById(teamId);
        verify(calendarRawService).getTeamCalendar(team);
        verify(teamMemberRawService).checkTeamAndMember(team, member);
        verify(eventRawService).findUpcomingEventsByCalendar(anyList());
    }
    
    @Test
    @DisplayName("오늘의 팀 일정 조회 성공")
    void getTodayTeamEvents_success() {
        // given
        String email = "team@test.com";
        Long teamId = 1L;
        
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        
        Calendar teamCalendar = spy(TestUtil.makeTeamCalendar(member, team));
        when(teamCalendar.getCalendarId()).thenReturn(100L);
        
        Event event1 = Event.builder()
            .title("오늘 팀 회의")
            .content("오늘 점심 팀 회의")
            .startAt(LocalDateTime.now().withHour(12))
            .endAt(LocalDateTime.now().withHour(13))
            .state(EventState.CONFIRMED)
            .isPrivate(false)
            .build();
        
        Event event2 = Event.builder()
            .title("오늘 팀 스터디")
            .content("오늘 저녁 팀 스터디")
            .startAt(LocalDateTime.now().withHour(19))
            .endAt(LocalDateTime.now().withHour(21))
            .state(EventState.CONFIRMED)
            .isPrivate(false)
            .build();
        
        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        when(teamRawService.findTeamById(teamId)).thenReturn(team);
        when(calendarRawService.getTeamCalendar(team)).thenReturn(teamCalendar);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        when(eventRawService.findTodayEventsByCalendar(anyList())).thenReturn(List.of(event1, event2));
        
        // when
        List<EventGetResponseDto> result = teamEventService.getTodayTeamEvents(email, teamId);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("오늘 팀 회의");
        assertThat(result.get(1).title()).isEqualTo("오늘 팀 스터디");
        
        verify(memberRawService).findMemberByEmail(email);
        verify(teamRawService).findTeamById(teamId);
        verify(calendarRawService).getTeamCalendar(team);
        verify(teamMemberRawService).checkTeamAndMember(team, member);
        verify(eventRawService).findTodayEventsByCalendar(anyList());
    }
    
}
