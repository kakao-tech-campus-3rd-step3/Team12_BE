package unischedule.calendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.calendar.entity.Calendar;
import unischedule.member.entity.Member;

import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, Long> {
    List<Calendar> findByOwner(Member owner);

    Optional<Calendar> findByOwnerAndTeamIsNull(Member owner);
}
