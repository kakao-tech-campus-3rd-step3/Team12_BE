package unischedule.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.service.TeamEventService;

@RestController
@RequestMapping("/api/events/team")
@RequiredArgsConstructor
public class TeamEventController {
    private final TeamEventService teamEventService;

    @PostMapping("/add")
    public ResponseEntity<EventCreateResponseDto> createTeamEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @RequestBody
            TeamEventCreateRequestDto requestDto
    ) {
        EventCreateResponseDto responseDto = teamEventService.createTeamEvent(userDetails.getUsername(), requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PatchMapping("/modify")
    public ResponseEntity<EventGetResponseDto> modifyTeamEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @RequestBody
            EventModifyRequestDto requestDto
    ) {
        EventGetResponseDto responseDto = teamEventService.modifyTeamEvent(userDetails.getUsername(), requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteTeamEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @PathVariable
            Long eventId
    ) {
        teamEventService.deleteTeamEvent(userDetails.getUsername(), eventId);

        return ResponseEntity.noContent().build();
    }
}
