package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.dto.EventOverrideDto;
import unischedule.events.repository.EventOverrideRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventOverrideRawService {
    private final EventOverrideRepository eventOverrideRepository;

    @Transactional(readOnly = true)
    public Optional<EventOverride> findEventOverride(Event originalEvent, LocalDateTime originalEventTime) {
        return eventOverrideRepository.findByOriginEventTime(originalEvent, originalEventTime);
    }

    @Transactional
    public EventOverride saveEventOverride(EventOverride eventOverride) {
        return eventOverrideRepository.save(eventOverride);
    }

    @Transactional
    public void updateEventOverride(EventOverride eventOverride, EventOverrideDto eventOverrideDto) {
        eventOverride.update(
                eventOverrideDto.originalStartTime(),
                eventOverrideDto.title(),
                eventOverrideDto.content(),
                eventOverrideDto.startTime(),
                eventOverrideDto.endTime(),
                eventOverrideDto.isPrivate()
        );
    }

    @Transactional
    public void deleteAllEventOverrideByEvent(Event event) {
        eventOverrideRepository.deleteAllByOriginalEvent(event);
    }
}
