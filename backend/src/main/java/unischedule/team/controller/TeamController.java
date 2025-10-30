package unischedule.team.controller;

import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import unischedule.common.dto.PageResponseDto;
import unischedule.common.dto.PaginationRequestDto;
import unischedule.team.dto.RemoveMemberCommandDto;
import unischedule.team.dto.TeamCreateRequestDto;
import unischedule.team.dto.TeamCreateResponseDto;
import unischedule.team.dto.TeamJoinRequestDto;
import unischedule.team.dto.TeamJoinResponseDto;
import unischedule.team.dto.TeamResponseDto;
import unischedule.team.dto.WhenToMeetRecommendDto;
import unischedule.team.dto.WhenToMeetResponseDto;
import unischedule.team.service.TeamService;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    ResponseEntity<TeamCreateResponseDto> createTeam(
            @RequestBody @Valid TeamCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TeamCreateResponseDto responseDto = teamService.createTeam(userDetails.getUsername(), requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @PostMapping("/join")
    ResponseEntity<TeamJoinResponseDto> joinTeam(
            @RequestBody @Valid TeamJoinRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        TeamJoinResponseDto responseDto = teamService.joinTeam(userDetails.getUsername(), requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{teamId}/member")
        //임시 주소 매핑
    ResponseEntity<Void> withdrawTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId
    ) {
        teamService.withdrawTeam(userDetails.getUsername(), teamId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{teamId}/team")
        //임시 주소 매핑
    ResponseEntity<Void> closeTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId
    ) {
        teamService.closeTeam(userDetails.getUsername(), teamId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/{teamId}/when-to-meet")
    ResponseEntity<List<WhenToMeetResponseDto>> getTeamMembersWhenToMeet(
            @PathVariable Long teamId
    ) {
        List<WhenToMeetResponseDto> whenToMeetList = teamService.getTeamMembersWhenToMeet(teamId);
        return ResponseEntity.ok(whenToMeetList);
    }
    
    @GetMapping("/{teamId}/when-to-meet/recommend")
    public ResponseEntity<List<WhenToMeetRecommendDto>> getOptimalTimeWhenToMeet(
        @RequestParam("start_time") LocalDateTime startTime,
        @RequestParam("end_time") LocalDateTime endTime,
        @RequestParam("required_time") Long requiredTime,
        @RequestParam("N") Long requiredCnt,
        @PathVariable Long teamId
    ) {
        List<WhenToMeetRecommendDto> result = teamService.getOptimalTimeWhenToMeet(
            startTime, endTime, requiredTime, requiredCnt, teamId
        );
        
        return ResponseEntity.ok(result);
    }
    @GetMapping
    public ResponseEntity<PageResponseDto<TeamResponseDto>> findMyTeamsWithMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search
    ) {
        PaginationRequestDto paginationInfo = new PaginationRequestDto(page, limit, search);
        PageResponseDto<TeamResponseDto> responseDto = teamService.findMyTeamsWithMembers(userDetails.getUsername(), paginationInfo);

        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<Void> removeMemberFromTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long teamId,
            @PathVariable Long memberId
    ) {
        RemoveMemberCommandDto requestDto = new RemoveMemberCommandDto(userDetails.getUsername(), teamId, memberId);
        teamService.removeMemberFromTeam(requestDto);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
