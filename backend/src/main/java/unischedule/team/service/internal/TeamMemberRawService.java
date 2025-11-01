package unischedule.team.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.common.dto.PaginationRequestDto;
import unischedule.exception.ConflictException;
import unischedule.exception.EntityNotFoundException;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.repository.TeamMemberRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamMemberRawService {
    private final TeamMemberRepository teamMemberRepository;

    /**
     * 팀 멤버 관계 저장
     *
     * @param relation
     * @return
     */
    @Transactional
    public TeamMember saveTeamMember(TeamMember relation) {
        return teamMemberRepository.save(relation);
    }

    /**
     * 특정 팀의 특정 멤버 검색
     *
     * @param team
     * @param member
     * @return
     */
    @Transactional(readOnly = true)
    public TeamMember findByTeamAndMember(Team team, Member member) {
        return teamMemberRepository.findByTeamAndMember(team, member)
                .orElseThrow(() -> new EntityNotFoundException("[" + member.getNickname() + "] 사용자가 해당 팀 소속이 아닙니다."));
    }

    @Transactional(readOnly = true)
    public void checkTeamAndMember(Team team, Member member) {
        if (!teamMemberRepository.existsByTeamAndMember(team, member)) {
            throw new EntityNotFoundException("사용자가 해당 팀 소속이 아닙니다.");
        }
    }

    @Transactional(readOnly = true)
    public void validateDuplication(Team team, Member member) {
        if (teamMemberRepository.existsByTeamAndMember(team, member)) {
            throw new ConflictException("이미 가입된 멤버입니다.");
        }
    }

    /**
     * 팀 멤버 검증
     *
     * @param team
     * @param member
     */
    @Transactional(readOnly = true)
    public void validateMembership(Team team, Member member) {
        if (!teamMemberRepository.existsByTeamAndMember(team, member)) {
            throw new EntityNotFoundException("사용자가 해당 팀 소속이 아닙니다.");
        }
    }

    /**
     * 멤버가 속한 팀 조회
     *
     * @param member
     * @return
     */
    @Transactional(readOnly = true)
    public List<TeamMember> findByMember(Member member) {
        return teamMemberRepository.findByMember(member);
    }

    /**
     * 팀의 멤버들을 페이징 처리하여 조회하는 메서드
     *
     * @param team           팀 엔티티 (조회 대상)
     * @param paginationInfo 페이징 및 검색 정보
     * @return 팀의 멤버들의 페이징된 결과
     */
    @Transactional(readOnly = true)
    public Page<TeamMember> getTeamMembersByTeam(Team team, PaginationRequestDto paginationInfo) {
        Pageable pageable = PageRequest.of(paginationInfo.page() - 1, paginationInfo.limit(), Sort.by(Sort.Order.by("role")));
        String keyword = paginationInfo.search();

        if (keyword == null || keyword.isBlank()) {
            return teamMemberRepository.findTeamMemberByTeam(team, pageable);
        }

        return teamMemberRepository.findTeamMemberByTeamAndKeyword(team, pageable, keyword);
    }

    /**
     * 팀의 멤버 조회
     *
     * @param team
     * @return
     */
    @Transactional(readOnly = true)
    public List<TeamMember> findByTeam(Team team) {
        return teamMemberRepository.findByTeam(team);
    }

    /**
     * 팀 멤버 수 조회
     *
     * @param team 팀 엔티티
     * @return 팀 멤버 수
     */
    @Transactional(readOnly = true)
    public int countTeamMemberByTeam(Team team) {
        return teamMemberRepository.countTeamMemberByTeam(team);
    }

    public int countByTeamAndRole(Team team, TeamRole role) {
        return teamMemberRepository.countByTeamAndRole(team, role);
    }

    @Transactional
    public void deleteTeamMember(TeamMember teamMember) {
        teamMemberRepository.delete(teamMember);
    }

    @Transactional
    public void deleteAllByMember(Member member) {
        teamMemberRepository.deleteAllByMember(member);
    }

    @Transactional
    public void deleteTeamMemberAll(List<TeamMember> teamMemberList) {
        teamMemberRepository.deleteAll(teamMemberList);
    }
}
