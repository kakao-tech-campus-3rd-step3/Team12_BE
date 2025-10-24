package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.collection.SingleEventList;
import unischedule.events.dto.EventUpdateDto;
import unischedule.events.repository.EventRepository;
import unischedule.exception.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventRawService {
    private final EventRepository eventRepository;

    private final static Boolean fromRecurring = false;

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
    public SingleEventList findSingleSchedule(List<Long> calendarIds, LocalDateTime startTime, LocalDateTime endTime) {
        List<Event> singleEventList = eventRepository.findSingleEventsInPeriod(calendarIds, startTime, endTime);

        return new SingleEventList(singleEventList);
    }

    @Transactional(readOnly = true)
    public boolean existsSingleSchedule(List<Long> calendarIds, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.existsSingleScheduleInPeriod(calendarIds, startTime, endTime);
    }
}
