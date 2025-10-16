package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventException;
import unischedule.events.repository.EventExceptionRepository;

@Service
@RequiredArgsConstructor
public class EventExceptionRawService {
    private final EventExceptionRepository eventExceptionRepository;

    @Transactional
    public EventException saveEventException(EventException eventException) {
        return eventExceptionRepository.save(eventException);
    }

    @Transactional
    public void deleteAllEventExceptionByEvent(Event event) {
        eventExceptionRepository.deleteAllByOriginalEvent(event);
    }
}
