package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.entity.Event;
import unischedule.events.repository.EventRepository;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventDomainService {
    private final EventRepository eventRepository;

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

    @Transactional(readOnly = true)
    public void validateNoSchedule(Member member, LocalDateTime startTime, LocalDateTime endTime) {
        if (eventRepository.existsPersonalScheduleInPeriod(member.getMemberId(), startTime, endTime)) {
            throw new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<Event> findSchedule(Member member, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findPersonalScheduleInPeriod(member.getMemberId(), startTime, endTime);
    }

    @Transactional(readOnly = true)
    public void canUpdateEvent(Member member, LocalDateTime startTime, LocalDateTime endTime, Event event) {
        if (eventRepository.existsPersonalScheduleInPeriodExcludingEvent(member.getMemberId(), startTime, endTime, event.getEventId())) {
            throw new InvalidInputException("해당 시간에 겹치는 일정이 있어 수정할 수 없습니다.");
        }
    }
}
