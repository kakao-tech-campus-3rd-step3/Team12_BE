package unischedule.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.parameters.P;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.service.TeamEventService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events/team")
@RequiredArgsConstructor
public class TeamEventController {
    private final TeamEventService teamEventService;

    @PostMapping("/add")
    public ResponseEntity<EventCreateResponseDto> createTeamEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @Valid @RequestBody
            TeamEventCreateRequestDto requestDto
    ) {
        EventCreateResponseDto responseDto = teamEventService.createTeamEvent(userDetails.getUsername(), requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/{teamId}")
    public ResponseEntity<List<EventGetResponseDto>> getTeamEvents(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @PathVariable
            Long teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endAt
    ) {
        List<EventGetResponseDto> responseDto = teamEventService.getTeamEvents(userDetails.getUsername(), teamId, startAt, endAt);
        return ResponseEntity.ok(responseDto);

    }

    @PatchMapping("/modify")
    public ResponseEntity<EventGetResponseDto> modifyTeamEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @Valid @RequestBody
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
