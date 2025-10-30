package unischedule.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventParticipant;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {
    void deleteAllByEvent(Event event);
}
