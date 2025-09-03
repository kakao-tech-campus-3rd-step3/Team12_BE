package unischedule.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

}
