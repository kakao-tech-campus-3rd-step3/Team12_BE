package unischedule.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.member.domain.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);
}
