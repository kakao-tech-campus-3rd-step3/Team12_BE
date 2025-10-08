package unischedule.team.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.common.dto.PaginationRequestDto;
import unischedule.exception.EntityNotFoundException;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;
import unischedule.team.repository.TeamRepository;

@Service
@RequiredArgsConstructor
public class TeamRawService {
    private final TeamRepository teamRepository;

    @Transactional
    public Team saveTeam(Team team) {
        return teamRepository.save(team);
    }

    @Transactional(readOnly = true)
    public Team findTeamById(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("해당 팀이 존재하지 않습니다."));
    }

    @Transactional(readOnly = true)
    public Team findTeamByInviteCode(String inviteCode) {
        return teamRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new EntityNotFoundException("요청한 정보의 팀이 없습니다."));
    }

    @Transactional(readOnly = true)
    public boolean existsByInviteCode(String inviteCode) {
        return teamRepository.existsByInviteCode(inviteCode);
    }

    @Transactional
    public void deleteTeam(Team team) {
        teamRepository.delete(team);
    }

    /**
     * 멤버가 속한 팀들을 페이징 처리하여 조회하는 메서드
     *
     * @param member         멤버 엔티티 (조회 대상)
     * @param paginationInfo 페이징 및 검색 정보
     * @return 멤버가 속한 팀들의 페이징된 결과
     */
    public Page<Team> findTeamsByMember(Member member, PaginationRequestDto paginationInfo) {
        Pageable pageable = PageRequest.of(paginationInfo.page() - 1, paginationInfo.limit(), Sort.by("teamId").ascending());
        String keyword = paginationInfo.search();

        if (keyword == null || keyword.isBlank()) {
            return teamRepository.findTeamsByMember(member, pageable);
        }

        return teamRepository.findTeamsByMemberAndKeyword(member, pageable, keyword);
    }
}
