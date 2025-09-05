package unischedule.users.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.users.entity.Calendar;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

}
