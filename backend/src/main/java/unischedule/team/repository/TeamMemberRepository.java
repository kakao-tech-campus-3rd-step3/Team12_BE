package unischedule.team.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeamAndMember(Team team, Member member);

    List<TeamMember> findByTeam(Team team);

    List<TeamMember> findByMember(Member member);

    Boolean existsByTeamAndMember(Team findTeam, Member findMember);
}
