package unischedule.team.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.repository.TeamMemberRepository;
import unischedule.team.repository.TeamRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamRawService {
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    /**
     * Member가 속한 팀 조회
     * @param member
     * @return
     */
    @Transactional(readOnly = true)
    public List<Team> findTeamByMember(Member member) {
        List<TeamMember> teamList = teamMemberRepository.findByMember(member);

        return teamList.stream()
                .map(TeamMember::getTeam)
                .toList();
    }
}
