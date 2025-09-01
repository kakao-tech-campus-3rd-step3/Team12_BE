package unischedule.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.users.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

}
