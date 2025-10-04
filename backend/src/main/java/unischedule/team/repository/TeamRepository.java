package unischedule.team.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByInviteCode(String code);

    Optional<Team> findByInviteCode(String s);

    @Query("""
            SELECT DISTINCT t
            FROM Team t
            JOIN TeamMember tm ON tm.team = t
            WHERE tm.member = :member
            AND (:keyword IS NULL OR t.name LIKE %:keyword%)
            """)
    Page<Team> findTeamByMemberWithNullableKeyword(
            @Param("member") Member member,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
