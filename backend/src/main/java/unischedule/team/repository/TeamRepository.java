package unischedule.team.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.team.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
