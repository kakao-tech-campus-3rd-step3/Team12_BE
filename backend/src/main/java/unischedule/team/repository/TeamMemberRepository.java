package unischedule.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.team.entity.TeamMember;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

}
