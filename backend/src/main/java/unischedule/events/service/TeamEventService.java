package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.service.common.EventCommandService;
import unischedule.events.service.common.EventQueryService;
import unischedule.events.service.internal.EventParticipantRawService;
import unischedule.events.service.internal.EventRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TeamEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final CalendarRawService calendarRawService;
    private final TeamRawService teamRawService;
    private final TeamMemberRawService teamMemberRawService;
    private final EventQueryService eventQueryService;
    private final EventCommandService eventCommandService;
    private final EventParticipantRawService eventParticipantRawService;

    @Transactional
    public EventCreateResponseDto createTeamSingleEvent(String email, TeamEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(requestDto.teamId());
        validateTeamMember(team, member);
        Calendar calendar = calendarRawService.getTeamCalendar(team);

        List<Member> participants = getParticipants(team, requestDto.eventParticipants());

        checkConflictForMembers(
                participants,
                requestDto.startTime(),
                requestDto.endTime()
        );

        Event savedEvent = eventCommandService.createSingleEvent(calendar, requestDto.toDto());

        handleEventParticipants(savedEvent, participants, requestDto.eventParticipants());

        return EventCreateResponseDto.from(savedEvent);
    }

    @Transactional
    public EventCreateResponseDto createTeamRecurringEvent(String email, Long teamId, RecurringEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(teamId);
        validateTeamMember(team, member);
        Calendar calendar = calendarRawService.getTeamCalendar(team);

        List<Member> participants = getParticipants(team, requestDto.eventParticipants());
        checkRecurringConflictForMembers(
                participants,
                requestDto.firstStartTime(),
                requestDto.firstEndTime(),
                requestDto.rrule()
        );

        Event savedEvent = eventCommandService.createRecurringEvent(calendar, requestDto);

        handleEventParticipants(savedEvent, participants, requestDto.eventParticipants());

        return EventCreateResponseDto.from(savedEvent);
    }

    @Transactional(readOnly = true)
    public EventGetResponseDto getTeamEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        event.validateIsTeamEvent();

        Team team = event.getCalendar().getTeam();
        validateTeamMember(team, member);

        if (event.getRecurrenceRule() == null) {
            return EventGetResponseDto.fromSingleEvent(event);
        }
        else {
            return EventGetResponseDto.fromRecurringEvent(event);
        }
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getTeamEvents(String email, Long teamId, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(teamId);

        validateTeamMember(team, member);

        Calendar teamCalendar = calendarRawService.getTeamCalendar(team);

        return eventQueryService.getEvents(List.of(teamCalendar.getCalendarId()), startAt, endAt)
                .stream()
                .map(EventGetResponseDto::fromServiceDto)
                .toList();
    }

    @Transactional
    public EventGetResponseDto modifyTeamEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);
        Team team = event.getCalendar().getTeam();
        event.validateIsTeamEvent();
        validateTeamMember(team, member);

        checkConflictForModification(event, team, requestDto);

        Event modifiedEvent = eventCommandService.modifySingleEvent(event, requestDto.toDto());

        if (requestDto.eventParticipants() != null) {
            List<Member> participants = getParticipants(team, requestDto.eventParticipants());
            handleEventParticipants(modifiedEvent, participants, requestDto.eventParticipants());
        }

        return EventGetResponseDto.fromSingleEvent(event);
    }

    @Transactional
    public EventGetResponseDto modifyTeamRecurringEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        event.validateIsTeamEvent();
        Team team = event.getCalendar().getTeam();
        validateTeamMember(team, member);

        checkConflictForModification(event, team, requestDto);

        Event modifiedEvent = eventCommandService.modifyRecurringEvent(
                event,
                requestDto.toDto()
        );

        if (requestDto.eventParticipants() != null) {
            List<Member> participants = getParticipants(team, requestDto.eventParticipants());
            handleEventParticipants(modifiedEvent, participants, requestDto.eventParticipants());
        }

        return EventGetResponseDto.fromRecurringEvent(event);
    }

    @Transactional
    public EventGetResponseDto modifyTeamRecurringInstance(String email, Long eventId, RecurringInstanceModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);
        originalEvent.validateIsTeamEvent();
        Team team = originalEvent.getCalendar().getTeam();
        validateTeamMember(team, member);

        // 반복 단건 수정 일정 충돌 체크
        checkConflictForRecurringInstanceModification(
                originalEvent,
                team,
                requestDto
        );

        EventOverride eventOverride = eventCommandService.modifyRecurringInstance(originalEvent, requestDto);

        return EventGetResponseDto.fromEventOverride(eventOverride, originalEvent);
    }

    @Transactional
    public void deleteTeamEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        Team team = event.getCalendar().getTeam();
        event.validateIsTeamEvent();
        validateTeamMember(team, member);
        eventCommandService.deleteSingleEvent(event);
    }
    
    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getTodayTeamEvents(String email, Long teamId) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        return getTeamEventsForPeriod(email, teamId, start, end);
    }
    
    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getUpcomingTeamEvents(String email, Long teamId) {
        LocalDateTime start = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(8).atStartOfDay();
        return getTeamEventsForPeriod(email, teamId, start, end);
    }

    @Transactional
    public void deleteTeamRecurringEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);
        event.validateIsTeamEvent();
        Team team = event.getCalendar().getTeam();
        validateTeamMember(team, member);

        eventCommandService.deleteRecurringEvent(event);
    }

    @Transactional
    public void deleteTeamRecurringInstance(String email, Long eventId, RecurringInstanceDeleteRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);
        originalEvent.validateIsTeamEvent();
        Team team = originalEvent.getCalendar().getTeam();

        validateTeamMember(team, member);

        eventCommandService.deleteRecurringEventInstance(originalEvent, requestDto);
    }

    private void validateTeamMember(Team team, Member member) {
        teamMemberRawService.checkTeamAndMember(team, member);
    }

    /**
     * 팀 이벤트 참여자 검증 및 반환
     * @param team
     * @param participantMemberIds
     * @return
     */
    private List<Member> getParticipants(Team team, List<Long> participantMemberIds) {
        if (participantMemberIds == null || participantMemberIds.isEmpty()) {
            return getAllTeamMember(team);
        }

        Set<Long> memberIdSet = new HashSet<>(participantMemberIds);
        List<Member> participants = new ArrayList<>();

        for (Long memberid : memberIdSet) {
            Member member = memberRawService.findMemberById(memberid);

            teamMemberRawService.checkTeamAndMember(team, member);
            participants.add(member);
        }
        return participants;
    }

    private List<Member> getCurrentParticipants(Event event, Team team) {
        if (event.isForAllMembers()) {
            return getAllTeamMember(team);
        }
        else {
            return eventParticipantRawService.getParticipantsForEvent(event);
        }
    }

    private void handleEventParticipants(Event event, List<Member> allParticipants, List<Long> newParticipantIds) {
        if (newParticipantIds == null) {
            return;
        }

        eventParticipantRawService.deleteAllParticipantsByEvent(event);

        if (newParticipantIds.isEmpty()) {
            event.updateIsSelective(false);
        }
        else {
            event.updateIsSelective(true);

            eventParticipantRawService.saveAllParticipantsForEvent(event, allParticipants);
        }
    }
    
    private List<EventGetResponseDto> getTeamEventsForPeriod(String email, Long teamId, LocalDateTime start, LocalDateTime end) {
        Member member = memberRawService.findMemberByEmail(email);
        Team team = teamRawService.findTeamById(teamId);
        
        validateTeamMember(team, member);
        
        Long calendarId = calendarRawService.getTeamCalendar(team).getCalendarId();
        List<EventServiceDto> events = eventQueryService.getEvents(List.of(calendarId), start, end);
        
        return events.stream()
            .map(EventGetResponseDto::fromServiceDto)
            .toList();
    }
    
    private List<Member> getAllTeamMember(Team team) {
        return teamMemberRawService.findByTeam(team)
                .stream()
                .map(TeamMember::getMember)
                .toList();
    }

    private void checkConflictForMembers(List<Member> participants, LocalDateTime startAt, LocalDateTime endAt) {
        for (Member participant : participants) {
            List<Long> calendarIds = getMemberCalendarIds(participant);
            eventQueryService.checkNewSingleEventOverlapForMember(
                    participant,
                    calendarIds,
                    startAt,
                    endAt
            );
        }
    }

    private void checkRecurringConflictForMembers(List<Member> participants, LocalDateTime startAt, LocalDateTime endAt, String rrule) {
        for (Member participant : participants) {
            List<Long> calendarIds = getMemberCalendarIds(participant);
            eventQueryService.checkNewRecurringEventOverlapForMember(
                    participant,
                    calendarIds,
                    startAt,
                    endAt,
                    rrule
            );
        }
    }

    private void checkConflictForModification(Event event, Team team, EventModifyRequestDto requestDto) {
        LocalDateTime newStartAt = getValueOrDefault(requestDto.startTime(), event.getStartAt());
        LocalDateTime newEndAt = getValueOrDefault(requestDto.endTime(), event.getEndAt());

        boolean timeChanged = (requestDto.startTime() != null || requestDto.endTime() != null);

        List<Member> membersToCheck;
        if (requestDto.eventParticipants() == null) {
            if (!timeChanged) return;
            membersToCheck = getCurrentParticipants(event, team);
        }
        else {
            membersToCheck = getParticipants(team, requestDto.eventParticipants());
        }

        for (Member participant : membersToCheck) {
            List<Long> calendarIds = getMemberCalendarIds(participant);
            eventQueryService.checkEventUpdateOverlapForMember(
                    participant,
                    calendarIds,
                    newStartAt,
                    newEndAt,
                    event
            );
        }
    }

    private void checkConflictForRecurringInstanceModification(Event event, Team team, RecurringInstanceModifyRequestDto requestDto) {
        boolean timeChanged = (requestDto.startTime() != null || requestDto.endTime() != null);
        if (!timeChanged) return;

        LocalDateTime newStartAt = getValueOrDefault(requestDto.startTime(), event.getStartAt());
        LocalDateTime newEndAt = getValueOrDefault(requestDto.endTime(), event.getEndAt());

        List<Member> currentParticipants = getCurrentParticipants(event, team);

        for (Member participant : currentParticipants) {
            List<Long> calendarIds = getMemberCalendarIds(participant);
            eventQueryService.checkEventUpdateOverlapForMember(participant, calendarIds, newStartAt, newEndAt, event);
        }
    }

    private <T> T getValueOrDefault(T value, T defaultValue) {
        if (value != null) return value;
        return defaultValue;
    }

    private List<Long> getMemberCalendarIds(Member member) {
        List<Team> teamList = teamMemberRawService.findByMember(member)
                .stream()
                .map(TeamMember::getTeam)
                .toList();

        List<Long> calendarIds = new ArrayList<>();
        calendarIds.add(calendarRawService.getMyPersonalCalendar(member).getCalendarId());
        List<Long> teamCalendarIds = teamList.stream()
                .map(calendarRawService::getTeamCalendar)
                .map(Calendar::getCalendarId)
                .toList();

        calendarIds.addAll(teamCalendarIds);
        return calendarIds;
    }
}
