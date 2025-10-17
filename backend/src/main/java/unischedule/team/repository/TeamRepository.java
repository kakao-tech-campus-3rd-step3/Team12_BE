package unischedule.team.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByInviteCode(String code);

    Optional<Team> findByInviteCode(String s);

    @Query("""
            SELECT DISTINCT t
            FROM Team t
            JOIN TeamMember tm ON tm.team = t
            WHERE tm.member = :member
            """)
    Page<Team> findTeamsByMember(Member member, Pageable pageable);


    @Query("""
            SELECT DISTINCT t
            FROM Team t
            JOIN TeamMember tm ON tm.team = t
            WHERE tm.member = :member
            AND (t.name LIKE %:keyword% OR t.description LIKE %:keyword%)
            """)
    Page<Team> findTeamsByMemberAndKeyword(Member member, Pageable pageable, String keyword);
}
