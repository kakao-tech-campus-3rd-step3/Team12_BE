package unischedule.team.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.team.domain.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    boolean existsByInviteCode(String code);
    
    Optional<Team> findByInviteCode(String s);
}
