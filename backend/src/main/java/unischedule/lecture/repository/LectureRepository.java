package unischedule.lecture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unischedule.lecture.domain.Lecture;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    
    @Query("SELECT l FROM Lecture l WHERE l.event.calendar.owner.memberId = :memberId AND l.endDate >= :today")
    List<Lecture> findActiveLecturesByMemberId(@Param("memberId") Long memberId, @Param("today") LocalDate today);
}

