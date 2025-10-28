package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.dto.EventOverrideUpdateDto;
import unischedule.events.repository.EventOverrideRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventOverrideRawService {
    private final EventOverrideRepository eventOverrideRepository;

    @Transactional(readOnly = true)
    public Optional<EventOverride> findEventOverride(Event originalEvent, LocalDateTime modifiedStartTime) {
        return eventOverrideRepository.findByEventStartTime(originalEvent, modifiedStartTime);
    }

    @Transactional(readOnly = true)
    public boolean existsDeleteEventOverride(Event originalEvent, LocalDateTime originalEventTime) {
        return eventOverrideRepository.existsByOriginalEventAndOriginalEventTimeAndTitleIsNull(
                originalEvent,
                originalEventTime
        );
    }

    @Transactional
    public EventOverride saveEventOverride(EventOverride eventOverride) {
        return eventOverrideRepository.save(eventOverride);
    }

    @Transactional
    public void updateEventOverride(EventOverride eventOverride, EventOverrideUpdateDto updateDto) {
        eventOverride.update(
                updateDto.title(),
                updateDto.content(),
                updateDto.startTime(),
                updateDto.endTime(),
                updateDto.isPrivate()
        );
    }

    @Transactional
    public void deleteAllEventOverrideByEvent(Event event) {
        eventOverrideRepository.deleteAllByOriginalEvent(event);
    }
}
