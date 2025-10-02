package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.dto.PersonalEventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.domain.EventState;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.domain.Event;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalEventService {
    private final MemberRawService memberDomainService;
    private final EventRawService eventDomainService;
    private final CalendarRawService calendarDomainService;

    @Transactional
    public EventCreateResponseDto makePersonalEvent(String email, PersonalEventCreateRequestDto requestDto) {
        Member member = memberDomainService.findMemberByEmail(email);

        Calendar targetCalendar = calendarDomainService.getMyPersonalCalendar(member);

        targetCalendar.validateOwner(member);

        eventDomainService.validateNoSchedule(member, requestDto.startTime(), requestDto.endTime());

        Event newEvent = Event.builder()
                .title(requestDto.title())
                .content(requestDto.description())
                .startAt(requestDto.startTime())
                .endAt(requestDto.endTime())
                .state(EventState.CONFIRMED)
                .isPrivate(requestDto.isPrivate())
                .build();

        newEvent.connectCalendar(targetCalendar);
        Event saved = eventDomainService.saveEvent(newEvent);

        return EventCreateResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getPersonalEvents(String email, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberDomainService.findMemberByEmail(email);

        List<Event> findEvents = eventDomainService.findSchedule(
                member,
                startAt,
                endAt
        );

        return findEvents.stream()
                .map(EventGetResponseDto::from)
                .toList();
    }
    
    @Transactional
    public EventGetResponseDto modifyPersonalEvent(String email, EventModifyRequestDto requestDto) {
        Member member = memberDomainService.findMemberByEmail(email);

        Event findEvent = eventDomainService.findEventById(requestDto.eventId());

        log.error("asdf");
        findEvent.validateEventOwner(member);

        validateUpdateTime(member, findEvent, requestDto.startTime(), requestDto.endTime());

        findEvent.modifyEvent(requestDto);
        
        return EventGetResponseDto.from(findEvent);
    }

    private void validateUpdateTime(Member member, Event event, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null && endTime == null) return;

        LocalDateTime newStartTime = Objects.requireNonNullElse(startTime, event.getStartAt());
        LocalDateTime newEndTime = Objects.requireNonNullElse(endTime, event.getEndAt());

        eventDomainService.canUpdateEvent(member, event, newStartTime, newEndTime);
    }

    @Transactional
    public void deletePersonalEvent(String email, Long eventId) {
        Member member = memberDomainService.findMemberByEmail(email);

        Event event = eventDomainService.findEventById(eventId);

        event.validateEventOwner(member);

        eventDomainService.deleteEvent(event);
    }
    
    public List<EventGetResponseDto> getUpcomingMyEvent(String email) {
        Member member = memberDomainService.findMemberByEmail(email);
        
        List<Event> upcomingEvents = eventDomainService.findUpcomingEventsByMember(member);
        
        return upcomingEvents.stream().map(EventGetResponseDto::from).toList();
    }
    
    public List<EventGetResponseDto> getTodayMyEvent(String email) {
        Member member = memberDomainService.findMemberByEmail(email);
        
        List<Event> todayEvents = eventDomainService.findTodayEventsByMember(member);
        
        return todayEvents.stream().map(EventGetResponseDto::from).toList();
    }
}
