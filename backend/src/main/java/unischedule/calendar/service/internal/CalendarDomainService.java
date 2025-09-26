package unischedule.calendar.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.exception.EntityNotFoundException;
import unischedule.member.entity.Member;
import unischedule.team.entity.Team;

@Service
@RequiredArgsConstructor
public class CalendarDomainService {
    private final CalendarRepository calendarRepository;

    @Transactional(readOnly = true)
    public Calendar findCalendarById(Long calendarId) {
        return calendarRepository.findById(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("해당 캘린더를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Calendar getMyPersonalCalendar(Member member) {
        return calendarRepository.findByOwnerAndTeamIsNull(member)
                .orElseThrow(() -> new EntityNotFoundException("개인 캘린더를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Calendar getTeamCalendar(Team team) {
        return calendarRepository.findByTeam(team)
                .orElseThrow(() -> new EntityNotFoundException("팀 캘린더를 찾을 수 없습니다."));
    }
}
