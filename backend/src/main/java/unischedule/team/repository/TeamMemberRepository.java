package unischedule.team.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;

import java.util.List;
import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeamAndMember(Team team, Member member);

    List<TeamMember> findByTeam(Team team);

    List<TeamMember> findByMember(Member member);

    Boolean existsByTeamAndMember(Team findTeam, Member findMember);

    Page<TeamMember> findTeamMemberByTeam(Team team, Pageable pageable);
}
