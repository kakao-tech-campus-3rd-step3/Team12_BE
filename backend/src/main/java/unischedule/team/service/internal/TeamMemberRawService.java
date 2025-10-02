package unischedule.team.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.repository.TeamMemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamMemberRawService {
    private final TeamMemberRepository teamMemberRepository;

    /**
     * 멤버가 속한 팀 조회
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

    /**
     * 팀의 멤버 조회
     * @param team
     * @return
     */
    @Transactional(readOnly = true)
    public List<Member> findMemberByTeam(Team team) {
        List<TeamMember> memberList = teamMemberRepository.findByTeam(team);

        return memberList.stream()
                .map(TeamMember::getMember)
                .toList();
    }
}
