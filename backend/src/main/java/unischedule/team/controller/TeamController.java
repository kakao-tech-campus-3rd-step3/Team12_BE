package unischedule.team.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unischedule.team.dto.TeamCreateRequestDto;
import unischedule.team.dto.TeamCreateResponseDto;
import unischedule.team.dto.TeamJoinRequestDto;
import unischedule.team.dto.TeamJoinResponseDto;
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
    
    @DeleteMapping("/{teamId}/member") //임시 주소 매핑
    ResponseEntity<Void> withdrawTeam(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long teamId
    ) {
        teamService.withdrawTeam(userDetails.getUsername(), teamId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @DeleteMapping("/{teamId}/team") //임시 주소 매핑
    ResponseEntity<Void> closeTeam(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long teamId
    ) {
        teamService.closeTeam(userDetails.getUsername(), teamId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
