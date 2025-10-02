package unischedule.team.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.exception.EntityNotFoundException;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.repository.TeamMemberRepository;
import unischedule.team.repository.TeamRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamRawService {
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public Team findTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 팀이 존재하지 않습니다."));
    }
}
