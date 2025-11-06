package unischedule.event.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.dto.EventCreateDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.dto.EventUpdateDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.dto.TeamEventGetResponseDto;
import unischedule.events.service.TeamEventService;
import unischedule.events.service.common.EventCommandService;
import unischedule.events.service.common.EventQueryService;
import unischedule.events.service.internal.EventParticipantRawService;
import unischedule.events.service.internal.EventRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;
import unischedule.util.TestUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
    @Mock
    private EventQueryService eventQueryService;
    @Mock
    private EventCommandService eventCommandService;
    @Mock
    private EventParticipantRawService eventParticipantRawService;
    @InjectMocks
    private TeamEventService teamEventService;

    @Test
    @DisplayName("팀 단일 일정 생성 (전체 멤버)")
    void createTeamEvent() {
        // given
        String email = "test@example.com";
        Long teamId = 1L;
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Calendar teamCalendar = TestUtil.makeTeamCalendar(member, team);

        TeamEventCreateRequestDto requestDto = new TeamEventCreateRequestDto(
                teamId, "팀 회의", "주간 보고",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), null);

        EventCreateDto createDto = requestDto.toDto();
        Event savedEvent = TestUtil.makeEvent("팀 회의", "주간 보고");

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(teamRawService.findTeamById(requestDto.teamId())).willReturn(team);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        given(calendarRawService.getTeamCalendar(team)).willReturn(teamCalendar);


        List<TeamMember> teamMembers = List.of(new TeamMember(team, member, TeamRole.MEMBER));
        given(teamMemberRawService.findByTeam(team)).willReturn(teamMembers);

        given(eventCommandService.createSingleEvent(eq(teamCalendar), any(EventCreateDto.class)))
                .willReturn(savedEvent);

        // when
        EventCreateResponseDto responseDto = teamEventService.createTeamSingleEvent(email, requestDto);

        // then
        assertThat(responseDto.title()).isEqualTo("팀 회의");
        assertThat(responseDto.description()).isEqualTo("주간 보고");

        verify(memberRawService).findMemberByEmail(email);
        verify(teamRawService).findTeamById(teamId);
        verify(teamMemberRawService).checkTeamAndMember(team, member);
        verify(calendarRawService).getTeamCalendar(team);
        verify(teamMemberRawService).findByTeam(team);
        verify(eventCommandService).createSingleEvent(eq(teamCalendar), any(EventCreateDto.class));
    }

    @Test
    @DisplayName("팀 반복 일정 생성 (선택 멤버)")
    void createTeamRecurringEvent() {
        // given
        String email = "test@test.com";
        Long teamId = 1L;
        Member member = TestUtil.makeMember();
        ReflectionTestUtils.setField(member, "memberId", 1L);
        Team team = TestUtil.makeTeam();
        Calendar teamCalendar = TestUtil.makeTeamCalendar(member, team);

        List<Long> participantIds = List.of(1L);
        RecurringEventCreateRequestDto requestDto = new RecurringEventCreateRequestDto(
                "팀 반복 회의",
                "매주 진행",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                "FREQ=WEEKLY;INTERVAL=1",
                participantIds
        );
        Event savedEvent = TestUtil.makeRecurringEvent("팀 반복 회의", "매주");

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(teamRawService.findTeamById(teamId)).willReturn(team);
        given(calendarRawService.getTeamCalendar(team)).willReturn(teamCalendar);

        given(memberRawService.findMemberById(1L)).willReturn(member);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);

        given(eventCommandService.createRecurringEvent(eq(teamCalendar), eq(requestDto)))
                .willReturn(savedEvent);

        doNothing().when(eventParticipantRawService).deleteAllParticipantsByEvent(savedEvent);
        doNothing().when(eventParticipantRawService).saveAllParticipantsForEvent(eq(savedEvent), anyList());

        // when
        EventCreateResponseDto result = teamEventService.createTeamRecurringEvent(email, teamId, requestDto);

        // then
        assertThat(result.title()).isEqualTo("팀 반복 회의");

        verify(memberRawService).findMemberByEmail(email);
        verify(teamRawService).findTeamById(teamId);
        verify(teamMemberRawService, times(2)).checkTeamAndMember(team, member);
        verify(calendarRawService).getTeamCalendar(team);
        verify(memberRawService).findMemberById(1L);
        verify(eventCommandService).createRecurringEvent(eq(teamCalendar), eq(requestDto));
        verify(eventParticipantRawService).deleteAllParticipantsByEvent(savedEvent);
        verify(eventParticipantRawService).saveAllParticipantsForEvent(eq(savedEvent), anyList());
    }

    @Test
    @DisplayName("팀 반복 일정 단건 수정 (시간 변경 없음)")
    void modifyTeamRecurringInstance() {
        // given
        String email = "test@test.com";
        Long eventId = 1L;
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Event originalEvent = TestUtil.makeRecurringEvent("팀 반복", "");
        Calendar teamCalendar = TestUtil.makeTeamCalendar(member, team);
        originalEvent.connectCalendar(teamCalendar);
        LocalDateTime originalStartTime = originalEvent.getStartAt().plusWeeks(1);

        RecurringInstanceModifyRequestDto requestDto = new RecurringInstanceModifyRequestDto(
                originalStartTime,
                "수정된 팀 회의",
                null,
                null,
                null
        );

        EventOverride savedOverride = mock(EventOverride.class);
        when(savedOverride.getTitle()).thenReturn("수정된 팀 회의");

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(eventRawService.findEventById(eventId)).willReturn(originalEvent);

        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);

        given(eventCommandService.modifyRecurringInstance(eq(originalEvent), eq(requestDto)))
                .willReturn(savedOverride);

        // when
        EventGetResponseDto result = teamEventService.modifyTeamRecurringInstance(email, eventId, requestDto);

        // then
        assertThat(result.title()).isEqualTo("수정된 팀 회의");
        assertThat(result.isRecurring()).isTrue();
        verify(memberRawService).findMemberByEmail(email);
        verify(eventRawService).findEventById(eventId);
        verify(teamMemberRawService).checkTeamAndMember(team, member);
        verify(eventCommandService).modifyRecurringInstance(eq(originalEvent), eq(requestDto));
        verify(eventQueryService, never()).checkEventUpdateOverlapForMember(any(), anyList(), any(), any(), any());
    }

    @Test
    @DisplayName("팀 반복 일정 단건 재수정 (시간 변경 O)")
    void modifyTeamRecurringInstance_Update() {
        // given
        String email = "test@test.com";
        Long eventId = 1L;
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Event originalEvent = TestUtil.makeRecurringEvent("팀 반복", "주간 회의");
        Calendar teamCalendar = TestUtil.makeTeamCalendar(member, team);
        originalEvent.connectCalendar(teamCalendar);
        LocalDateTime originalStartTime = originalEvent.getStartAt().plusWeeks(1);
        LocalDateTime newStartTime = originalStartTime.plusHours(2); // 시간 변경
        LocalDateTime newEndTime = newStartTime.plusHours(1);

        RecurringInstanceModifyRequestDto requestDto = new RecurringInstanceModifyRequestDto(
                originalStartTime,
                "두 번째 수정",
                "내용도 수정",
                newStartTime,
                newEndTime
        );

        EventOverride updatedOverride = mock(EventOverride.class);
        when(updatedOverride.getTitle()).thenReturn("두 번째 수정");
        when(updatedOverride.getContent()).thenReturn("내용도 수정");
        when(updatedOverride.getStartAt()).thenReturn(newStartTime);
        when(updatedOverride.getEndAt()).thenReturn(newEndTime);

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(eventRawService.findEventById(eventId)).willReturn(originalEvent);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);

        List<TeamMember> teamMembers = List.of(new TeamMember(team, member, TeamRole.MEMBER));
        given(eventCommandService.modifyRecurringInstance(eq(originalEvent), eq(requestDto)))
                .willReturn(updatedOverride);

        // when
        EventGetResponseDto result = teamEventService.modifyTeamRecurringInstance(email, eventId, requestDto);

        // then
        verify(memberRawService).findMemberByEmail(email);
        verify(eventRawService).findEventById(eventId);
        verify(teamMemberRawService).checkTeamAndMember(team, member);

        verify(eventCommandService).modifyRecurringInstance(eq(originalEvent), eq(requestDto));

        assertThat(result.title()).isEqualTo("두 번째 수정");
        assertThat(result.description()).isEqualTo("내용도 수정");
        assertThat(result.isRecurring()).isTrue();
    }

    @Test
    @DisplayName("팀 일정 수정 (참여자 변경: 선택 -> 전체)")
    void modifyTeamEvent() {
        // given
        String email = "test@example.com";
        Long eventId = 1L;
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Calendar teamCalendar = TestUtil.makeTeamCalendar(member, team);
        Event event = spy(TestUtil.makeEvent("Original Title", "Original Content"));
        event.connectCalendar(teamCalendar);
        EventModifyRequestDto requestDto = new EventModifyRequestDto(
                "Updated Title", "Updated Content", event.getStartAt(), event.getEndAt(), Collections.emptyList());

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(eventRawService.findEventById(eventId)).willReturn(event);

        doNothing().when(event).validateIsTeamEvent();
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);

        List<TeamMember> teamMembers = List.of(new TeamMember(team, member, TeamRole.MEMBER));
        given(teamMemberRawService.findByTeam(team)).willReturn(teamMembers);

        doAnswer(invocation -> {
            Event eventToModify = invocation.getArgument(0);
            EventUpdateDto updateDto = invocation.getArgument(1);
            eventToModify.modifyEvent(updateDto.title(),updateDto.content(), updateDto.startTime(), updateDto.endTime());
            return eventToModify;
        }).when(eventCommandService).modifySingleEvent(eq(event), any(EventUpdateDto.class));

        doNothing().when(eventParticipantRawService).deleteAllParticipantsByEvent(event);

        // when
        EventGetResponseDto responseDto = teamEventService.modifyTeamEvent(email, eventId, requestDto);

        // then
        assertThat(responseDto.title()).isEqualTo("Updated Title");
        assertThat(responseDto.description()).isEqualTo("Updated Content");

        verify(memberRawService).findMemberByEmail(email);
        verify(eventRawService).findEventById(eventId);
        verify(event).validateIsTeamEvent();
        verify(teamMemberRawService).checkTeamAndMember(team, member);

        verify(teamMemberRawService).findByTeam(team);
        verify(eventCommandService).modifySingleEvent(eq(event), any(EventUpdateDto.class));
        verify(eventParticipantRawService).deleteAllParticipantsByEvent(event);
        verify(event).updateIsSelective(false);
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

        doNothing().when(eventCommandService).deleteSingleEvent(eq(event));

        // when
        teamEventService.deleteTeamEvent(email, eventId);

        // then
        verify(memberRawService).findMemberByEmail(email);
        verify(eventRawService).findEventById(eventId);
        verify(teamCalendar).getTeam();
        verify(event).validateIsTeamEvent();
        verify(teamMemberRawService).checkTeamAndMember(team, member);
        verify(eventCommandService).deleteSingleEvent(eq(event));
    }

    @Test
    @DisplayName("팀 반복 일정 특정 인스턴스 삭제 - EventCommandService 호출 확인")
    void deleteTeamRecurringInstance() {
        // given
        String email = "test@example.com";
        Long eventId = 4L;
        Member member = TestUtil.makeMember();
        Team team = TestUtil.makeTeam();
        Calendar teamCalendar = TestUtil.makeTeamCalendar(member, team);
        Event originalEvent = spy(TestUtil.makeRecurringEvent("반복 일정", "매주"));
        originalEvent.connectCalendar(teamCalendar);
        LocalDateTime originalStartTime = originalEvent.getStartAt().plusWeeks(1);
        RecurringInstanceDeleteRequestDto requestDto = new RecurringInstanceDeleteRequestDto(originalStartTime);

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(eventRawService.findEventById(eventId)).willReturn(originalEvent);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        doNothing().when(eventCommandService).deleteRecurringEventInstance(eq(originalEvent), eq(requestDto));

        // when
        teamEventService.deleteTeamRecurringInstance(email, eventId, requestDto);

        // then
        verify(memberRawService).findMemberByEmail(email);
        verify(eventRawService).findEventById(eventId);
        verify(originalEvent).validateIsTeamEvent();
        verify(teamMemberRawService).checkTeamAndMember(team, member);
        verify(eventCommandService).deleteRecurringEventInstance(eq(originalEvent), eq(requestDto));
    }

    @Test
    @DisplayName("다가오는 팀 일정 조회 성공")
    void getUpcomingTeamEvents_success() {
        // given
        String email = "team@test.com";
        Long teamId = 1L;

        Member member1 = TestUtil.makeMember();
        ReflectionTestUtils.setField(member1, "memberId", 123L);
        Member member2 = TestUtil.makeMember();
        ReflectionTestUtils.setField(member2, "memberId", 456L);

        Team team = TestUtil.makeTeam();
        Calendar teamCalendar = spy(TestUtil.makeTeamCalendar(member1, team));
        when(teamCalendar.getCalendarId()).thenReturn(100L);

        List<TeamMember> teamMemberList = List.of(
                new TeamMember(team, member1, TeamRole.MEMBER),
                new TeamMember(team, member2, TeamRole.MEMBER)
        );

        EventServiceDto event1 = new EventServiceDto(
                1L,
                "선택 참여 회의",
                "주간 회의",
                LocalDateTime.of(2025, 9, 10, 10, 0),
                LocalDateTime.of(2025, 9, 10, 11, 0),
                false
        );

        EventServiceDto event2 = new EventServiceDto(
                2L,
                "전체 참여 워크샵",
                "분기별 워크샵",
                LocalDateTime.of(2025, 9, 15, 14, 0),
                LocalDateTime.of(2025, 9, 15, 17, 0),
                false
        );

        LocalDateTime start = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(8).atStartOfDay();

        when(memberRawService.findMemberByEmail(email)).thenReturn(member1);
        when(teamRawService.findTeamById(teamId)).thenReturn(team);
        when(calendarRawService.getTeamCalendar(team)).thenReturn(teamCalendar);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member1);
        when(eventQueryService.getEvents(anyList(), eq(start), eq(end))).thenReturn(List.of(event1, event2));

        // 선택 참여
        Event event1_entity = spy(TestUtil.makeEvent("선택 참여 회의", ""));
        when(event1_entity.isForAllMembers()).thenReturn(false);
        when(eventRawService.findEventById(1L)).thenReturn(event1_entity);
        when(eventParticipantRawService.getParticipantsForEvent(event1_entity)).thenReturn(List.of(member1));

        // 2번 이벤트 (전체 참여)
        Event event2_entity = spy(TestUtil.makeEvent("전체 참여 워크샵", ""));
        when(event2_entity.isForAllMembers()).thenReturn(true);
        when(event2_entity.getCalendar()).thenReturn(teamCalendar);
        when(eventRawService.findEventById(2L)).thenReturn(event2_entity);
        when(teamMemberRawService.findByTeam(team)).thenReturn(teamMemberList);

        // when
        List<TeamEventGetResponseDto> result = teamEventService.getUpcomingTeamEvents(email, teamId);

        // then
        assertThat(result.get(0).title()).isEqualTo("선택 참여 회의");
        assertThat(result.get(0).eventParticipants())
                .describedAs("member1만 포함해야 함")
                .containsExactly(123L);

        assertThat(result.get(1).title()).isEqualTo("전체 참여 워크샵");
        assertThat(result.get(1).eventParticipants())
                .describedAs("member1, member2 포함")
                .containsExactlyInAnyOrder(123L, 456L);

        verify(memberRawService).findMemberByEmail(email);
        verify(teamRawService).findTeamById(teamId);
        verify(calendarRawService).getTeamCalendar(team);
        verify(teamMemberRawService).checkTeamAndMember(team, member1);
        verify(eventQueryService).getEvents(anyList(), eq(start), eq(end));
        verify(eventRawService).findEventById(1L);
        verify(eventRawService).findEventById(2L);
        verify(eventParticipantRawService).getParticipantsForEvent(event1_entity);
        verify(teamMemberRawService).findByTeam(team);
    }

    @Test
    @DisplayName("오늘의 팀 일정 조회 성공")
    void getTodayTeamEvents_success() {
        // given
        String email = "team@test.com";
        Long teamId = 1L;

        Member member = TestUtil.makeMember();
        ReflectionTestUtils.setField(member, "memberId", 123L);
        Team team = TestUtil.makeTeam();

        Calendar teamCalendar = spy(TestUtil.makeTeamCalendar(member, team));
        when(teamCalendar.getCalendarId()).thenReturn(100L);

        List<TeamMember> teamMemberList = List.of(new TeamMember(team, member, TeamRole.MEMBER));

        EventServiceDto event1 = new EventServiceDto(
                1L,
                "오늘 회의",
                "주간 회의",
                LocalDateTime.of(2025, 9, 10, 10, 0),
                LocalDateTime.of(2025, 9, 10, 11, 0),
                false
        );

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();

        when(memberRawService.findMemberByEmail(email)).thenReturn(member);
        when(teamRawService.findTeamById(teamId)).thenReturn(team);
        when(calendarRawService.getTeamCalendar(team)).thenReturn(teamCalendar);
        doNothing().when(teamMemberRawService).checkTeamAndMember(team, member);
        when(eventQueryService.getEvents(anyList(), eq(start), eq(end))).thenReturn(List.of(event1));

        Event event1_entity = spy(TestUtil.makeEvent("오늘 회의", ""));
        when(event1_entity.isForAllMembers()).thenReturn(true);
        when(event1_entity.getCalendar()).thenReturn(teamCalendar);
        when(eventRawService.findEventById(1L)).thenReturn(event1_entity);
        when(teamMemberRawService.findByTeam(team)).thenReturn(teamMemberList);

        // when
        List<TeamEventGetResponseDto> result = teamEventService.getTodayTeamEvents(email, teamId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().title()).isEqualTo("오늘 회의");
        assertThat(result.getFirst().eventParticipants())
                .describedAs("member1 포함")
                .containsExactly(123L);

        verify(memberRawService).findMemberByEmail(email);
        verify(teamRawService).findTeamById(teamId);
        verify(calendarRawService).getTeamCalendar(team);
        verify(teamMemberRawService).checkTeamAndMember(team, member);
        verify(eventQueryService).getEvents(anyList(), eq(start), eq(end));
        verify(eventRawService).findEventById(1L);
        verify(teamMemberRawService).findByTeam(team);
    }

}
