package unischedule.team.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.exception.ConflictException;
import unischedule.exception.EntityNotFoundException;
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
     * 팀 멤버 관계 저장
     *
     * @param relation
     * @return
     */
    @Transactional
    public TeamMember saveTeamMember(TeamMember relation) {
        return teamMemberRepository.save(relation);
    }

    /**
     * 특정 팀의 특정 멤버 검색
     *
     * @param team
     * @param member
     * @return
     */
    @Transactional(readOnly = true)
    public TeamMember findByTeamAndMember(Team team, Member member) {
        return teamMemberRepository.findByTeamAndMember(team, member)
                .orElseThrow(() -> new EntityNotFoundException("사용자가 해당 팀 소속이 아닙니다."));
    }

    @Transactional(readOnly = true)
    public void validateDuplication(Team team, Member member) {
        if (teamMemberRepository.existsByTeamAndMember(team, member)) {
            throw new ConflictException("이미 가입된 멤버입니다.");
        }
    }

    /**
     * 멤버가 속한 팀 조회
     *
     * @param member
     * @return
     */
    @Transactional(readOnly = true)
    public List<TeamMember> findByMember(Member member) {
        return teamMemberRepository.findByMember(member);
    }

    /**
     * 팀의 멤버 조회
     *
     * @param team
     * @return
     */
    @Transactional(readOnly = true)
    public List<TeamMember> findByTeam(Team team) {
        return teamMemberRepository.findByTeam(team);
    }

    @Transactional
    public void deleteTeamMember(TeamMember teamMember) {
        teamMemberRepository.delete(teamMember);
    }

    @Transactional
    public void deleteTeamMemberAll(List<TeamMember> teamMemberList) {
        teamMemberRepository.deleteAll(teamMemberList);
    }
}
