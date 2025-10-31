package unischedule.team.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.common.dto.PageResponseDto;
import unischedule.common.dto.PaginationRequestDto;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.domain.WhenToMeet;
import unischedule.team.dto.MemberNameResponseDto;
import unischedule.team.dto.RemoveMemberCommandDto;
import unischedule.team.dto.TeamCreateRequestDto;
import unischedule.team.dto.TeamCreateResponseDto;
import unischedule.team.dto.TeamDetailResponseDto;
import unischedule.team.dto.TeamJoinRequestDto;
import unischedule.team.dto.TeamJoinResponseDto;
import unischedule.team.dto.TeamMemberResponseDto;
import unischedule.team.dto.TeamResponseDto;
import unischedule.team.dto.WhenToMeetRecommendDto;
import unischedule.team.dto.WhenToMeetResponseDto;
import unischedule.team.service.internal.TeamCodeGenerator;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;
import unischedule.team.service.internal.WhenToMeetLogicService;
import unischedule.team.service.internal.WhenToMeetRawService;

/**
 * 팀과 관련한 서비스 메서드들의 클래스
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRawService teamRawService;
    private final CalendarRawService calendarRawService;
    private final MemberRawService memberRawService;
    private final TeamMemberRawService teamMemberRawService;
    private final WhenToMeetRawService whenToMeetRawService;
    private final WhenToMeetLogicService whenToMeetLogicService;
    private final TeamCodeGenerator teamCodeGenerator = new TeamCodeGenerator();

    /**
     * 팀 생성을 위한 메서드
     *
     * @param email      헤더에서 얻어온 유저 이메일
     * @param requestDto 생성 요청 Dto, 팀 이름에 대한 정보 포함
     * @return 팀 생성된 결과를 담은 Dto
     */
    @Transactional
    public TeamCreateResponseDto createTeam(String email, TeamCreateRequestDto requestDto) {

        Member findMember = memberRawService.findMemberByEmail(email);

        String code = generateTeamCode();

        Team newTeam = new Team(requestDto.teamName(), requestDto.teamDescription(), code);

        Team saved = teamRawService.saveTeam(newTeam);

        TeamMember relation = new TeamMember(saved, findMember, TeamRole.LEADER);
        teamMemberRawService.saveTeamMember(relation);

        Calendar teamCalendar = new Calendar(findMember, saved);

        calendarRawService.saveCalendar(teamCalendar);

        return new TeamCreateResponseDto(
                saved.getTeamId(),
                saved.getName(),
                saved.getDescription(),
                saved.getInviteCode()
        );
    }

    private String generateTeamCode() {
        String code;
        do {
            code = teamCodeGenerator.generate();
        } while (teamRawService.existsByInviteCode(code));
        return code;
    }

    /**
     *
     * @param email      헤더에서 넘어온 유저 이메일
     * @param requestDto 팀 코드 요청
     * @return 가입된 팀의 정보
     */
    public TeamJoinResponseDto joinTeam(String email, TeamJoinRequestDto requestDto) {

        Team findTeam = teamRawService.findTeamByInviteCode(requestDto.inviteCode());

        Member findMember = memberRawService.findMemberByEmail(email);

        teamMemberRawService.validateDuplication(findTeam, findMember);

        TeamMember relation = new TeamMember(findTeam, findMember, TeamRole.MEMBER);
        teamMemberRawService.saveTeamMember(relation);

        return new TeamJoinResponseDto(
                findTeam.getTeamId(),
                findTeam.getName(),
                findTeam.getDescription()
        );
    }

    /**
     * 팀 탈퇴
     *
     * @param email  이메일
     * @param teamId 팀 아이디
     */
    @Transactional
    public void withdrawTeam(String email, Long teamId) {
        Team findTeam = teamRawService.findTeamById(teamId);

        Member findMember = memberRawService.findMemberByEmail(email);

        TeamMember findRelation = teamMemberRawService.findByTeamAndMember(findTeam, findMember);
        
        /*
        팀 탈퇴 전 해야할 일이 생긴다면 여기 추가
         */

        teamMemberRawService.deleteTeamMember(findRelation);
    }

    /**
     * 팀 삭제
     *
     * @param email  이메일
     * @param teamId 팀 아이디
     */
    @Transactional
    public void closeTeam(String email, Long teamId) {
        Team findTeam = teamRawService.findTeamById(teamId);

        Member findMember = memberRawService.findMemberByEmail(email);

        Calendar findCalendar = calendarRawService.getTeamCalendar(findTeam);

        TeamMember findRelation = teamMemberRawService.findByTeamAndMember(findTeam, findMember);

        findRelation.checkLeader();
        
        /*
        팀 삭제 전 해야할 일이 생긴다면 여기 추가
         */

        List<TeamMember> findTeamMember = teamMemberRawService.findByTeam(findTeam);
        teamMemberRawService.deleteTeamMemberAll(findTeamMember);

        calendarRawService.deleteCalendar(findCalendar);

        teamRawService.deleteTeam(findTeam);
    }

    /**
     * 일정 겹치는 것 체크
     *
     * @param teamId
     * @return 겹치는 일정 리스트
     */
    //현재 "겹치는 일정"이 없다고 보고 만든 코드
    public List<WhenToMeetResponseDto> getTeamMembersWhenToMeet(Long teamId) {
        List<Member> members = whenToMeetRawService.findTeamMembers(teamId);
        List<LocalDateTime> starts = whenToMeetLogicService.generateIntervalStarts();
        List<LocalDateTime> ends = whenToMeetLogicService.generateIntervalEnds();

        List<WhenToMeet> slots = whenToMeetLogicService.generateSlots(members, starts, ends);
        whenToMeetLogicService.applyMemberEvents(slots, members, starts, ends, whenToMeetRawService);

        return whenToMeetLogicService.toResponse(slots);
    }

    /**
     * 사용자가 속한 팀들을 페이징 처리하여 조회하는 메서드
     *
     * @param email          헤더에서 넘어온 유저 이메일
     * @param paginationMeta 페이징 및 검색 정보
     * @return 사용자가 속한 팀들의 페이징된 결과
     */
    @Transactional(readOnly = true)
    public PageResponseDto<TeamResponseDto> findMyTeamsWithMembers(String email, PaginationRequestDto paginationMeta) {
        Member findMember = memberRawService.findMemberByEmail(email);
        Page<Team> findTeams = teamRawService.findTeamsByMember(findMember, paginationMeta);
        Page<TeamResponseDto> responseDtos = findTeams.map(team -> {
            List<MemberNameResponseDto> memberDtos = toMemberNameResponseDtos(teamMemberRawService.findByTeam(team));
            return new TeamResponseDto(
                    team.getTeamId(),
                    team.getName(),
                    memberDtos,
                    memberDtos.size(),
                    team.getInviteCode()
            );
        });

        return PageResponseDto.from(responseDtos);
    }

    /**
     * 팀의 멤버들을 페이징 처리하여 조회하는 메서드
     *
     * @param email          헤더에서 넘어온 유저 이메일
     * @param teamId         팀 아이디
     * @param paginationMeta 페이징 및 검색 정보
     * @return 팀의 멤버들의 페이징된 결과
     */
    @Transactional(readOnly = true)
    public PageResponseDto<TeamMemberResponseDto> getTeamMembers(String email, Long teamId, PaginationRequestDto paginationMeta) {
        Member findMember = memberRawService.findMemberByEmail(email);
        Team findTeam = teamRawService.findTeamById(teamId);
        teamMemberRawService.validateMembership(findTeam, findMember);
        Page<TeamMember> members = teamMemberRawService.getTeamMembersByTeam(findTeam, paginationMeta);
        Page<TeamMemberResponseDto> responseDtos = members.map(TeamMemberResponseDto::from);

        return PageResponseDto.from(responseDtos);
    }

    /**
     * TeamMember 리스트를 MemberNameResponseDto 리스트로 변환하는 메서드
     *
     * @param teamMembers 팀 멤버 엔티티 리스트
     * @return 멤버 이름 응답 Dto 리스트
     */
    private List<MemberNameResponseDto> toMemberNameResponseDtos(List<TeamMember> teamMembers) {

        return teamMembers.stream()
                .map(MemberNameResponseDto::from)
                .toList();
    }

    /**
     * 팀에서 멤버를 제거하는 메서드
     *
     * @param requestDto 요청 Dto, 리더 이메일, 팀 아이디, 제거할 멤버 아이디 포함
     */
    @Transactional
    public void removeMemberFromTeam(RemoveMemberCommandDto requestDto) {
        Team findTeam = teamRawService.findTeamById(requestDto.teamId());
        Member leaderMember = memberRawService.findMemberByEmail(requestDto.leaderEmail());
        Member targetMember = memberRawService.findMemberById(requestDto.targetMemberId());
        TeamMember leader = teamMemberRawService.findByTeamAndMember(findTeam, leaderMember);
        TeamMember target = teamMemberRawService.findByTeamAndMember(findTeam, targetMember);
        leader.checkLeader();
        target.validateRemovable();

        teamMemberRawService.deleteTeamMember(target);
    }
    
    public List<WhenToMeetRecommendDto> getOptimalTimeWhenToMeet(LocalDateTime startTime, LocalDateTime endTime, Long requiredTime, Long requiredCnt, Long teamId) {
        // 팀 멤버 조회
        List<Member> members = whenToMeetRawService.findTeamMembers(teamId);
        
        // 일주일 슬롯 생성
        List<LocalDateTime> intervalStarts = whenToMeetLogicService.generateIntervalStarts(startTime, endTime);
        List<LocalDateTime> intervalEnds = whenToMeetLogicService.generateIntervalEnds(startTime, endTime);
        List<WhenToMeet> slots = whenToMeetLogicService.generateSlots(members, intervalStarts, intervalEnds);
        
        // 멤버 이벤트 반영
        whenToMeetLogicService.applyMemberEvents(slots, members, intervalStarts, intervalEnds, whenToMeetRawService);
        
        // 추천 N개 계산
        return recommendBestSlots(slots, requiredTime, requiredCnt, Long.valueOf(members.size()));
    }
    
    public List<WhenToMeetRecommendDto> recommendBestSlots(
        List<WhenToMeet> slots,
        Long durationMinutes,
        Long topN,
        Long memberCnt) {
        
        int requiredSlots = (int) ((durationMinutes + 14) / 15);
        
        List<WhenToMeet> recommendedWindows = new ArrayList<>();
        
        // 하루별로 슬롯 그룹화
        Map<LocalDate, List<WhenToMeet>> slotsByDate = slots.stream()
            .collect(Collectors.groupingBy(s -> s.getStartTime().toLocalDate()));
        
        // 하루 단위 연속 슬롯 탐색
        for (List<WhenToMeet> daySlots : slotsByDate.values()) {
            for (int i = 0; i <= daySlots.size() - requiredSlots; i++) {
                List<WhenToMeet> windowSlots = daySlots.subList(i, i + requiredSlots);
                
                long minAvailable = windowSlots.stream()
                    .mapToLong(WhenToMeet::getAvailableMember)
                    .min()
                    .orElse(0);
                
                recommendedWindows.add(new WhenToMeet(
                    windowSlots.get(0).getStartTime(),
                    windowSlots.get(windowSlots.size() - 1).getEndTime(),
                    minAvailable
                ));
            }
        }
        
        return recommendedWindows.stream()
            .sorted((a, b) -> {
                int cmp = Long.compare(b.getAvailableMember(), a.getAvailableMember());
                if (cmp != 0) return cmp;
                return a.getStartTime().compareTo(b.getStartTime());
            })
            .limit(topN)
            .map(window -> WhenToMeetRecommendDto.from(window, memberCnt)) // DTO.from 호출
            .toList();
    }
  
    /**
     * 팀 상세 정보를 조회하는 메서드
     *
     * @param email  헤더에서 넘어온 유저 이메일
     * @param teamId 팀 아이디
     * @return 팀 상세 정보 응답 Dto
     */
    @Transactional(readOnly = true)
    public TeamDetailResponseDto getTeamDetail(String email, Long teamId) {
        Team findTeam = teamRawService.findTeamById(teamId);
        Member findMember = memberRawService.findMemberByEmail(email);
        teamMemberRawService.checkTeamAndMember(findTeam, findMember);
        int memberCount = teamMemberRawService.countTeamMemberByTeam(findTeam);

        return TeamDetailResponseDto.of(findTeam, memberCount);
    }
}
