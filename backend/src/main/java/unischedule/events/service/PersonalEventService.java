package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.RRule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventException;
import unischedule.events.domain.EventState;
import unischedule.events.domain.RecurrenceRule;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.repository.EventExceptionRepository;
import unischedule.events.service.internal.EventRawService;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonalEventService {
    private final MemberRawService memberRawService;
    private final EventRawService eventRawService;
    private final TeamMemberRawService teamMemberRawService;
    private final CalendarRawService calendarRawService;
    private final EventExceptionRepository eventExceptionRepository;

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

    @Transactional
    public EventCreateResponseDto makePersonalRecurringEvent(String email, RecurringEventCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);

        Calendar targetCalendar = calendarRawService.getMyPersonalCalendar(member);

        targetCalendar.validateOwner(member);

        eventRawService.validateNoScheduleForRecurrence(
                member,
                requestDto.firstStartTime(),
                requestDto.firstEndTime(),
                requestDto.rrule()
        );

        Event newEvent = Event.builder()
                .title(requestDto.title())
                .content(requestDto.description())
                .startAt(requestDto.firstStartTime())
                .endAt(requestDto.firstEndTime())
                .state(EventState.CONFIRMED)
                .isPrivate(requestDto.isPrivate())
                .build();

        RecurrenceRule rule = new RecurrenceRule(requestDto.rrule());
        newEvent.connectRecurrenceRule(rule);

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

        List<Event> findEvents = new ArrayList<>();
        List<Event> singleEvents = eventRawService.findSingleSchedule(calendarIds, startAt, endAt);
        findEvents.addAll(singleEvents);
        List<Event> recurringEvents = eventRawService.findRecurringSchedule(calendarIds, endAt);

        Map<Long, List<EventException>> exceptionsMap = eventExceptionRepository
                .findEventExceptionsForEvents(recurringEvents, startAt, endAt)
                .stream()
                .collect(Collectors.groupingBy(ex -> ex.getOriginalEvent().getEventId()));

        for (Event recurEvent : recurringEvents) {
            List<Event> occurrences = generateOccurrences(recurEvent, startAt, endAt);
            List<EventException> exceptions = exceptionsMap.getOrDefault(recurEvent.getEventId(), List.of());
            findEvents.addAll(applyExceptions(occurrences, exceptions));
        }

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
        
        List<Event> upcomingEvents = eventRawService.findUpcomingEventsByMember(member);
        
        return upcomingEvents.stream().map(EventGetResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getTodayMyEvent(String email) {
        Member member = memberRawService.findMemberByEmail(email);
        
        List<Event> todayEvents = eventRawService.findTodayEventsByMember(member);
        
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

    private List<Event> generateOccurrences(Event recEvent, LocalDateTime startAt, LocalDateTime endAt) {
        try {
            RRule<ZonedDateTime> rrule = new RRule<>(recEvent.getRecurrenceRule().getRruleString());
            Recur<ZonedDateTime> recur = rrule.getRecur();

            ZoneId zone = ZoneId.systemDefault();
            ZonedDateTime seed = recEvent.getStartAt().atZone(zone);
            ZonedDateTime startZdt = startAt.atZone(zone);
            ZonedDateTime endZdt = endAt.atZone(zone);

            Duration duration = Duration.between(seed.toLocalDateTime(), recEvent.getEndAt());

            List<ZonedDateTime> dates = recur.getDates(startZdt, endZdt);

            return dates.stream()
                    .filter(occurrenceStart -> !occurrenceStart.isBefore(startZdt) && occurrenceStart.isBefore(endZdt))
                    .map(occurrenceStart -> Event.builder()
                            .title(recEvent.getTitle())
                            .content(recEvent.getContent())
                            .startAt(occurrenceStart.toLocalDateTime())
                            .endAt(occurrenceStart.toLocalDateTime().plus(duration))
                            .isPrivate(recEvent.getIsPrivate())
                            .state(recEvent.getState())
                            .build())
                    .toList();
        }
        catch (RuntimeException e) {
            throw new InvalidInputException("유효하지 않은 반복 규칙(RRULE) 형식입니다.");
        }
    }

    private List<Event> applyExceptions(List<Event> occurrences, List<EventException> exceptions) {
        if (exceptions.isEmpty()) {
            return occurrences;
        }

        Map<LocalDateTime, EventException> exceptionMap = exceptions.stream()
                .collect(Collectors.toMap(EventException::getOriginalEventTime, ex -> ex));

        List<Event> finalOccurrences = new ArrayList<>();

        for (Event occurrence : occurrences) {
            if (exceptionMap.containsKey(occurrence.getStartAt())) {
                EventException exception = exceptionMap.get(occurrence.getStartAt());

                if (exception.getTitle() != null) {
                    finalOccurrences.add(Event.builder().title(exception.getTitle())
                            .content(exception.getContent())
                            .startAt(exception.getStartAt())
                            .endAt(exception.getEndAt())
                            .isPrivate(exception.getIsPrivate())
                            .build());
                }
            }
            else {
                finalOccurrences.add(occurrence);
            }
        }
        return finalOccurrences;
    }
}
