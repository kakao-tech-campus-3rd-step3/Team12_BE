package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.calendar.service.internal.CalendarDomainService;
import unischedule.events.dto.EventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.entity.EventState;
import unischedule.events.service.internal.EventDomainService;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.entity.Event;
import unischedule.events.repository.EventRepository;
import unischedule.member.entity.Member;
import unischedule.member.repository.MemberRepository;
import unischedule.member.service.internal.MemberDomainService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EventService {
    private final MemberDomainService memberDomainService;
    private final EventDomainService eventDomainService;
    private final CalendarDomainService calendarDomainService;

    @Transactional
    public EventCreateResponseDto makePersonalEvent(String email, EventCreateRequestDto requestDto) {
        Member member = memberDomainService.findMemberByEmail(email);

        Calendar targetCalendar = calendarDomainService.findCalendarById(requestDto.calendarId());

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

        findEvent.validateEventOwner(member);

        if (requestDto.startTime() != null || requestDto.endTime() != null) {
            LocalDateTime newStartAt = requestDto.startTime() != null ? requestDto.startTime() : findEvent.getStartAt();
            LocalDateTime newEndAt = requestDto.endTime() != null ? requestDto.endTime() : findEvent.getEndAt();

            eventDomainService.canUpdateEvent(member, newStartAt, newEndAt, findEvent);
        }
        
        findEvent.modifyEvent(requestDto);
        
        return EventGetResponseDto.from(findEvent);
    }

    @Transactional
    public void deletePersonalEvent(String email, Long eventId) {
        Member member = memberDomainService.findMemberByEmail(email);

        Event event = eventDomainService.findEventById(eventId);

        event.validateEventOwner(member);

        eventDomainService.deleteEvent(member, event);
    }
}
