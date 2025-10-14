package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventState;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.service.internal.EventRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonalEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final TeamMemberRawService teamMemberRawService;
    private final CalendarRawService calendarRawService;

    @Transactional
    public EventCreateResponseDto makePersonalEvent(String email, PersonalEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Calendar targetCalendar = calendarRawService.getMyPersonalCalendar(member);

        targetCalendar.validateOwner(member);

        eventRawService.validateNoSchedule(member, requestDto.startTime(), requestDto.endTime());

        Event newEvent = Event.builder()
                .title(requestDto.title())
                .content(requestDto.description())
                .startAt(requestDto.startTime())
                .endAt(requestDto.endTime())
                .state(EventState.CONFIRMED)
                .isPrivate(requestDto.isPrivate())
                .build();

        newEvent.connectCalendar(targetCalendar);
        Event saved = eventRawService.saveEvent(newEvent);

        return EventCreateResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getPersonalEvents(String email, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberRawService.findMemberByEmail(email);

        List<Team> teamList = teamMemberRawService.findByMember(member)
                .stream()
                .map(TeamMember::getTeam)
                .toList();

        List<Long> calendarIds = getMemberCalendarIds(teamList, member);

        List<Event> findEvents = eventRawService.findSchedule(
                calendarIds,
                startAt,
                endAt
        );

        return findEvents.stream()
                .map(EventGetResponseDto::from)
                .toList();
    }

    @Transactional
    public EventGetResponseDto modifyPersonalEvent(String email, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Event findEvent = eventRawService.findEventById(requestDto.eventId());

        findEvent.validateEventOwner(member);

        eventRawService.canUpdateEvent(member, findEvent, requestDto.startTime(), requestDto.endTime());

        eventRawService.updateEvent(findEvent, EventModifyRequestDto.toDto(requestDto));
        
        return EventGetResponseDto.from(findEvent);
    }

    @Transactional
    public void deletePersonalEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(eventId);

        event.validateEventOwner(member);

        eventRawService.deleteEvent(event);
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getUpcomingMyEvent(String email) {
        Member member = memberRawService.findMemberByEmail(email);
        
        List<Team> teamList = teamMemberRawService.findByMember(member)
            .stream()
            .map(TeamMember::getTeam)
            .toList();
        
        List<Long> calendarIds = getMemberCalendarIds(teamList, member);
        
        List<Event> upcomingEvents = eventRawService.findUpcomingEventsByCalendar(calendarIds);
        
        return upcomingEvents.stream().map(EventGetResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getTodayMyEvent(String email) {
        Member member = memberRawService.findMemberByEmail(email);
        
        List<Team> teamList = teamMemberRawService.findByMember(member)
            .stream()
            .map(TeamMember::getTeam)
            .toList();
        
        List<Long> calendarIds = getMemberCalendarIds(teamList, member);
        
        List<Event> todayEvents = eventRawService.findTodayEventsByCalendar(calendarIds);
        
        return todayEvents.stream().map(EventGetResponseDto::from).toList();
    }

    private List<Long> getMemberCalendarIds(List<Team> teamList, Member member) {
        List<Long> calendarIds = new ArrayList<>();

        // 팀 캘린더
        List<Long> teamCalendarIds = teamList.stream()
                .map(calendarRawService::getTeamCalendar)
                .map(Calendar::getCalendarId)
                .toList();

        // 개인 캘린더
        calendarIds.add(calendarRawService.getMyPersonalCalendar(member).getCalendarId());

        calendarIds.addAll(teamCalendarIds);
        return calendarIds;
    }
}
