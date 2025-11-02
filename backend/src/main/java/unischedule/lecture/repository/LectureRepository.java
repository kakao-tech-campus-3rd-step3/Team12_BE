package unischedule.lecture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.lecture.domain.Lecture;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END FROM Lecture l WHERE l.event.eventId = :eventId")
    boolean existsByEventId(@Param("eventId") Long eventId);
  
    List<Lecture> findByEventCalendarOwnerMemberIdAndEndDateGreaterThanEqual(Long memberId, LocalDate today);
}

