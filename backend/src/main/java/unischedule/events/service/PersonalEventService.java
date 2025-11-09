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
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.dto.PersonalEventGetResponseDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.service.common.EventCommandService;
import unischedule.events.service.common.EventQueryService;
import unischedule.events.service.internal.EventRawService;
import unischedule.lecture.service.internal.LectureRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PersonalEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final EventQueryService eventQueryService;
    private final TeamMemberRawService teamMemberRawService;
    private final CalendarRawService calendarRawService;
    private final EventCommandService eventCommandService;
    private final LectureRawService lectureRawService;

    @Transactional
    public EventCreateResponseDto makePersonalSingleEvent(String email, PersonalEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Calendar targetCalendar = calendarRawService.getMyPersonalCalendar(member);
        targetCalendar.validateOwner(member);

        Event saved = eventCommandService.createSingleEvent(targetCalendar, requestDto.toDto());

        return EventCreateResponseDto.from(saved);
    }

    @Transactional
    public EventCreateResponseDto makePersonalRecurringEvent(String email, RecurringEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Calendar targetCalendar = calendarRawService.getMyPersonalCalendar(member);
        targetCalendar.validateOwner(member);

        Event saved = eventCommandService.createRecurringEvent(targetCalendar, requestDto);

        return EventCreateResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PersonalEventGetResponseDto getPersonalEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);
        Event event = eventRawService.findEventById(eventId);

        event.validateEventOwner(member);

        Set<Long> lectureEventIds = lectureRawService.getAllLectureEventIds(email);
        String eventType = determineEventType(event, lectureEventIds);

        if (event.getRecurrenceRule() == null) {
            return PersonalEventGetResponseDto.fromSingleEvent(event, eventType);
        }
        else {
            return PersonalEventGetResponseDto.fromRecurringEvent(event, eventType);
        }
    }

    @Transactional(readOnly = true)
    public List<PersonalEventGetResponseDto> getPersonalEvents(String email, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberRawService.findMemberByEmail(email);

        List<Long> calendarIds = getMemberCalendarIds(member);

        Set<Long> lectureEventIds = lectureRawService.getAllLectureEventIds(email);

        List<EventServiceDto> serviceDtos = eventQueryService.getEventsForMember(member, calendarIds, startAt, endAt);

        return serviceDtos.stream()
                .map(serviceDto -> {
                    Event originalEvent = eventRawService.findEventById(serviceDto.eventId());
                    String eventType = determineEventType(originalEvent, lectureEventIds);
                    return PersonalEventGetResponseDto.fromServiceDto(serviceDto, eventType);
                })
                .toList();
    }

    @Transactional
    public EventGetResponseDto modifyPersonalEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event findEvent = eventRawService.findEventById(eventId);
        findEvent.validateEventOwner(member);

        eventCommandService.modifySingleEvent(findEvent, requestDto.toDto());

        return EventGetResponseDto.fromSingleEvent(findEvent);
    }

    @Transactional
    public EventGetResponseDto modifyPersonalRecurringEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event foundEvent = eventRawService.findEventById(eventId);
        foundEvent.validateEventOwner(member);

        eventCommandService.modifyRecurringEvent(foundEvent, requestDto.toDto());

        return EventGetResponseDto.fromRecurringEvent(foundEvent);
    }

    @Transactional
    public EventGetResponseDto modifyPersonalRecurringInstance(String email, Long eventId, RecurringInstanceModifyRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);
        originalEvent.validateEventOwner(member);

        EventOverride savedOverride = eventCommandService.modifyRecurringInstance(originalEvent, requestDto);

        return EventGetResponseDto.fromEventOverride(savedOverride, originalEvent);
    }

    @Transactional
    public void deletePersonalRecurringInstance(String email, Long eventId, RecurringInstanceDeleteRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        Event originalEvent = eventRawService.findEventById(eventId);

        originalEvent.validateEventOwner(member);

        eventCommandService.deleteRecurringEventInstance(originalEvent, requestDto);
    }

    @Transactional
    public void deletePersonalEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(eventId);

        event.validateEventOwner(member);

        eventCommandService.deleteSingleEvent(event);
    }

    @Transactional
    public void deleteRecurringEvent(String email, Long eventId) {
        Member member = memberRawService.findMemberByEmail(email);

        Event event = eventRawService.findEventById(eventId);

        event.validateEventOwner(member);

        eventCommandService.deleteRecurringEvent(event);
    }
    
    @Transactional(readOnly = true)
    public List<PersonalEventGetResponseDto> getTodayMyEvent(String email) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(1).atStartOfDay();
        List<PersonalEventGetResponseDto> allEvents = getPersonalEvents(email, start, end);
        
        return allEvents.stream()
            .sorted(Comparator.comparing(PersonalEventGetResponseDto::startTime))
            .toList();
    }
    
    @Transactional(readOnly = true)
    public List<PersonalEventGetResponseDto> getUpcomingMyEvent(String email) {
        LocalDateTime start = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime end = LocalDate.now().plusDays(8).atStartOfDay();
        
        List<PersonalEventGetResponseDto> allEvents = getPersonalEvents(email, start, end);
        Set<Long> lectureEventIds = lectureRawService.getAllLectureEventIds(email);
        
        return allEvents.stream()
            .filter(eventDto -> !lectureEventIds.contains(eventDto.eventId()))
            .sorted(Comparator.comparing(PersonalEventGetResponseDto::startTime))
            .toList();
    }
    
    private List<Long> getMemberCalendarIds(Member member) {
        List<Team> teamList = teamMemberRawService.findByMember(member)
                .stream()
                .map(TeamMember::getTeam)
                .toList();

        List<Long> calendarIds = new ArrayList<>();

        // 개인 캘린더
        calendarIds.add(calendarRawService.getMyPersonalCalendar(member).getCalendarId());

        // 팀 캘린더
        List<Long> teamCalendarIds = teamList.stream()
                .map(calendarRawService::getTeamCalendar)
                .map(Calendar::getCalendarId)
                .toList();

        calendarIds.addAll(teamCalendarIds);
        return calendarIds;
    }

    private <T> T getValueOrDefault(T value, T defaultValue) {
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    private String determineEventType(Event event, Set<Long> lectureEventIds) {
        if (lectureEventIds.contains(event.getEventId())) {
            return "class";
        }
        if (event.getCalendar() != null && event.getCalendar().hasTeam()) {
            return "team";
        }

        return "personal";
    }
}
