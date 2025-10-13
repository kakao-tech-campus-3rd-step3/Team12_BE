package unischedule.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.events.domain.EventException;

public interface EventExceptionRepository extends JpaRepository<EventException, Long> {
}
