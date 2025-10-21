package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.events.domain.Event;
import unischedule.events.dto.EventServiceDto;
import unischedule.events.dto.EventUpdateDto;
import unischedule.events.repository.EventRepository;
import unischedule.events.util.RRuleParser;
import unischedule.exception.EntityNotFoundException;
import unischedule.member.domain.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import unischedule.team.domain.Team;

@Service
@RequiredArgsConstructor
public class EventRawService {
    private final EventRepository eventRepository;
    private final RRuleParser rruleParser;

    private final Boolean fromRecurring = false;

    @Transactional
    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("해당 일정을 찾을 수 없습니다."));
    }

    @Transactional
    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }

    @Transactional
    public void updateEvent(Event event, EventUpdateDto updateDto) {
        event.modifyEvent(
                updateDto.title(),
                updateDto.content(),
                updateDto.startTime(),
                updateDto.endTime(),
                updateDto.isPrivate()
        );
    }

    @Transactional(readOnly = true)
    public List<Event> findSchedule(List<Long> calendarIds, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findEventsInCalendarsInPeriod(calendarIds, startTime, endTime);
    }

    @Transactional(readOnly = true)
    public List<EventServiceDto> findSingleSchedule(List<Long> calendarIds, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findSingleEventsInPeriod(calendarIds, startTime, endTime)
                .stream()
                .map(event -> EventServiceDto.fromSingleEvent(event, fromRecurring))
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean existsSingleSchedule(List<Long> calendarIds, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.existsSingleScheduleInPeriod(calendarIds, startTime, endTime);
    }
    
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsByCalendar(List<Long> calendarIds) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 3); // 개수 제한
        return eventRepository.findUpcomingEvents(calendarIds, now, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<Event> findTodayEventsByCalendar(List<Long> calendarIds) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();              // 오늘 00:00
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();    // 내일 00:00
        return eventRepository.findEventsInCalendarsInPeriod(calendarIds, startOfDay, endOfDay);
    }
}
