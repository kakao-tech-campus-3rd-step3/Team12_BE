package unischedule.team.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.member.entity.Member;
import unischedule.team.entity.Team;
import unischedule.team.entity.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    
    Optional<TeamMember> findByTeamAndMember(Team team, Member member);
    
    List<TeamMember> findByTeam(Team team);
    
    Boolean existsByTeamAndMember(Team findTeam, Member findMember);
}
