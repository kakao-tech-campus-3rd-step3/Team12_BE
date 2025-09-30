package unischedule.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.auth.entity.RefreshToken;
import unischedule.member.domain.Member;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByMember(Member member);
}
