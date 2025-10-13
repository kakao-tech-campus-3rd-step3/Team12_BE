package unischedule.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EventExceptionRepository extends JpaRepository<EventRepository, Long> {
}
