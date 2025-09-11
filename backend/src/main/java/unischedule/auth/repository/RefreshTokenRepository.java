package unischedule.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
