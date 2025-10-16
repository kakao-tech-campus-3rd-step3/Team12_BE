package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventException;
import unischedule.events.dto.EventExceptionDto;
import unischedule.events.repository.EventExceptionRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventExceptionRawService {
    private final EventExceptionRepository eventExceptionRepository;

    @Transactional(readOnly = true)
    public Optional<EventException> findEventException(Event originalEvent, LocalDateTime originalEventTime) {
        return eventExceptionRepository.findByOriginEventTime(originalEvent, originalEventTime);
    }

    @Transactional
    public EventException saveEventException(EventException eventException) {
        return eventExceptionRepository.save(eventException);
    }

    @Transactional
    public void updateEventException(EventException eventException, EventExceptionDto eventExceptionDto) {
        eventException.update(
                eventExceptionDto.originalStartTime(),
                eventExceptionDto.title(),
                eventExceptionDto.content(),
                eventExceptionDto.startTime(),
                eventExceptionDto.endTime(),
                eventExceptionDto.isPrivate()
        );
    }

    @Transactional
    public void deleteAllEventExceptionByEvent(Event event) {
        eventExceptionRepository.deleteAllByOriginalEvent(event);
    }
}
