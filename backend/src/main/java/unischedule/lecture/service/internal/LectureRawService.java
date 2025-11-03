package unischedule.lecture.service.internal;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.exception.EntityNotFoundException;
import unischedule.lecture.domain.Lecture;
import unischedule.lecture.repository.LectureRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureRawService {
    private final LectureRepository lectureRepository;
    
    @Transactional
    public Lecture saveLecture(Lecture lecture) {
        return lectureRepository.save(lecture);
    }
    
    @Transactional(readOnly = true)
    public Lecture findLectureById(Long lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new EntityNotFoundException("강의를 찾을 수 없습니다."));
    }
    
    @Transactional(readOnly = true)
    public List<Lecture> findActiveLecturesByMemberId(Long memberId) {
        return lectureRepository.findByEventCalendarOwnerMemberIdAndEndDateGreaterThanEqual(memberId, LocalDate.now());
    }
    
    @Transactional
    public void deleteLecture(Lecture lecture) {
        lectureRepository.delete(lecture);
    }
    
    @Transactional(readOnly = true)
    public Set<Long> getAllLectureEventIds(String email) {
        return lectureRepository.findAllEventIds(email);
    }
}

