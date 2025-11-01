package unischedule.lecture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.lecture.domain.Lecture;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LectureRepository extends JpaRepository<Lecture, Long> {
    
    List<Lecture> findByEventCalendarOwnerMemberIdAndEndDateGreaterThanEqual(Long memberId, LocalDate today);
}

