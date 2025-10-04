package unischedule.team.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.dto.TeamWithMembersFlatDto;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByTeamAndMember(Team team, Member member);

    List<TeamMember> findByTeam(Team team);

    List<TeamMember> findByMember(Member member);

    Boolean existsByTeamAndMember(Team findTeam, Member findMember);

    @Query(value = """
            SELECT new unischedule.team.dto.TeamWithMembersFlatDto(
            
                        )
            FROM TeamMember tm
            JOIN tm.team t
            JOIN tm.member m
            WHERE t.teamId IN (
                        SELECT tm2.team.teamId
                        FROM TeamMember tm2
                        WHERE tm2.member.memberId = :memberId
                        )
            AND (:keyword IS NULL OR t.name LIKE %:keyword%)
            """)
    Page<TeamWithMembersFlatDto> findTeamWithMembers(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
