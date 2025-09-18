package unischedule.team.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
        @RequestBody TeamCreateRequestDto requestDto,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        TeamCreateResponseDto responseDto = teamService.createTeam(userDetails.getUsername(), requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
    
    @PostMapping("/join")
    ResponseEntity<TeamJoinResponseDto> joinTeam(
        @RequestBody TeamJoinRequestDto requestDto,
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        TeamJoinResponseDto responseDto = teamService.joinTeam(userDetails.getUsername(), requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
