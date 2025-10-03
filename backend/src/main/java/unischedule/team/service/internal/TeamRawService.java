package unischedule.team.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.exception.EntityNotFoundException;
import unischedule.team.domain.Team;
import unischedule.team.repository.TeamRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamRawService {
    private final TeamRepository teamRepository;

    @Transactional
    public Team saveTeam(Team team) {
        return teamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public Team findTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 팀이 존재하지 않습니다."));
    }

    @Transactional(readOnly = true)
    public Team findTeamByInviteCode(String inviteCode) {
        return teamRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new EntityNotFoundException("요청한 정보의 팀이 없습니다."));
    }

    @Transactional(readOnly = true)
    public boolean existsByInviteCode(String inviteCode) {
        return teamRepository.existsByInviteCode(inviteCode);
    }

    @Transactional
    public void deleteTeam(Team team) {
        teamRepository.delete(team);
    }
}
