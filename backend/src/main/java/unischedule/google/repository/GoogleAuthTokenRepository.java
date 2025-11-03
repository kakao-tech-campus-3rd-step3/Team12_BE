package unischedule.google.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.google.domain.GoogleAuthToken;
import unischedule.member.domain.Member;

import java.util.Optional;

public interface GoogleAuthTokenRepository extends JpaRepository<GoogleAuthToken, Long> {

    Optional<GoogleAuthToken> findByMember(Member member);
}
