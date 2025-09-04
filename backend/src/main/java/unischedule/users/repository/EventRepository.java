package unischedule.users.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.users.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCreatorIdAndStartAtGreaterThanEqualAndEndAtLessThanEqual(
        Long creatorId, LocalDateTime startAt, LocalDateTime endAt);
}
