package unischedule.team.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unischedule.team.service.TeamService;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {
    
    private final TeamService teamService;
    
    @PostMapping
    ResponseEntity<TeamCreateResponseDto> createTeam(
        @RequestBody TeamCreateRequestDto requestDto
        //헤더에서 유저 정보 파싱해와 아이디 얻어오는거 추가 필요
    ) {
        TeamCreateResponseDto responseDto = teamService.createTeam(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
}
