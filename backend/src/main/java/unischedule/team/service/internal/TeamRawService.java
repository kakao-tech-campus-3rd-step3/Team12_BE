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
     * 멤버가 속한 팀을 페이징 처리하여 조회합니다.
     * 키워드가 null이거나 빈 문자열인 경우, 모든 팀을 조회합니다.
     * 그렇지 않으면 팀 이름이나 설명에 키워드가 포함된 팀을 조회합니다.
     *
     * @param member         조회할 멤버
     * @param paginationInfo 페이징 및 검색 정보를 담은 DTO
     * @return 조회된 팀의 페이지
     */
    public Page<Team> findTeamByMemberWithNullableKeyword(Member member, PaginationRequestDto paginationInfo) {
        Pageable pageable = PageRequest.of(paginationInfo.page() - 1, paginationInfo.limit(), Sort.by("teamId").ascending());
        String keyword = paginationInfo.search();

        return teamRepository.findTeamByMemberWithNullableKeyword(member, keyword, pageable);
    }
}
