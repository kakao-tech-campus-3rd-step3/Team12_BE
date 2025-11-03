package unischedule.lecture.repository;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unischedule.lecture.domain.Lecture;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    
    @Query("""
    SELECT l.event.eventId
    FROM Lecture l
    where l.event.calendar.owner.email = :email
    """)
    Set<Long> findAllEventIds(String email);
  
    List<Lecture> findByEventCalendarOwnerMemberIdAndEndDateGreaterThanEqual(Long memberId, LocalDate today);
}

