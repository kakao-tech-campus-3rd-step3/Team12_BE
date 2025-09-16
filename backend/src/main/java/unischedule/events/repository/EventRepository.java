package unischedule.events.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.events.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByStartAtLessThanAndEndAtGreaterThan(
        LocalDateTime endAt,
        LocalDateTime startAt
    );
    
    boolean existsByStartAtLessThanAndEndAtGreaterThan(
        LocalDateTime endAt,
        LocalDateTime startAt
    );
}
