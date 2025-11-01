package unischedule.google.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.google.domain.GoogleAuthToken;

public interface GoogleAuthTokenRepository extends JpaRepository<GoogleAuthToken, Long> {
}
