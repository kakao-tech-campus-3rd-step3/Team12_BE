package unischedule.calendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.calendar.entity.Calendar;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {

}
