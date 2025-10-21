package unischedule.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
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
            @RequestBody @Valid
            TeamEventCreateRequestDto requestDto
    ) {
        EventCreateResponseDto responseDto = teamEventService.createTeamSingleEvent(userDetails.getUsername(), requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/recurring/add/{teamId}")
    public ResponseEntity<EventCreateResponseDto> createTeamRecurringEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @PathVariable
            Long teamId,
            @RequestBody @Valid
            RecurringEventCreateRequestDto requestDto
    ) {
        EventCreateResponseDto responseDto = teamEventService.createTeamRecurringEvent(
                userDetails.getUsername(),
                teamId,
                requestDto
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<EventGetResponseDto> getTeamEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long eventId
    ) {
        EventGetResponseDto responseDto = teamEventService.getTeamEvent(userDetails.getUsername(), eventId);
        return ResponseEntity.ok(responseDto);
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
        List<EventGetResponseDto> responseDto = teamEventService.getTeamEvents(
                userDetails.getUsername(),
                teamId,
                startAt,
                endAt
        );
        return ResponseEntity.ok(responseDto);

    }

    @PatchMapping("/modify/{eventId}")
    public ResponseEntity<EventGetResponseDto> modifyTeamEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @PathVariable
            Long eventId,
            @RequestBody @Valid
            EventModifyRequestDto requestDto
    ) {
        EventGetResponseDto responseDto = teamEventService.modifyTeamEvent(
                userDetails.getUsername(),
                eventId,
                requestDto
        );
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/recurring/modify/{eventId}")
    public ResponseEntity<EventGetResponseDto> modifyTeamRecurringEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @PathVariable
            Long eventId,
            @RequestBody @Valid
            EventModifyRequestDto requestDto
    ) {
        EventGetResponseDto responseDto = teamEventService.modifyTeamRecurringEvent(
                userDetails.getUsername(),
                eventId,
                requestDto
        );
        return ResponseEntity.ok(responseDto);
    }

    @PatchMapping("/recurring/instance/{eventId}")
    public ResponseEntity<EventGetResponseDto> modifyTeamRecurringInstance(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @PathVariable
            Long eventId,
            @RequestBody @Valid
            RecurringInstanceModifyRequestDto requestDto
    ) {
        EventGetResponseDto responseDto = teamEventService.modifyTeamRecurringInstance(
                userDetails.getUsername(),
                eventId,
                requestDto
        );

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
    
    @GetMapping("/{teamId}/upcomming")
    public ResponseEntity<List<EventGetResponseDto>> upcomingTeamEvents(
        @AuthenticationPrincipal
        UserDetails userDetails,
        @PathVariable
        Long teamId
    ) {
        List<EventGetResponseDto> upcomingEvents = teamEventService.getUpcomingTeamEvents(
            userDetails.getUsername(), teamId
        );
        
        return ResponseEntity.ok(upcomingEvents);
    }
    
    @GetMapping("/{teamId}/today")
    public ResponseEntity<List<EventGetResponseDto>> todayTeamEvents(
        @AuthenticationPrincipal
        UserDetails userDetails,
        @PathVariable
        Long teamId
    ) {
        List<EventGetResponseDto> todayEvents = teamEventService.getTodayTeamEvents(
            userDetails.getUsername(), teamId
        );
        
        return ResponseEntity.ok(todayEvents);
    }
  
    @DeleteMapping("/recurring/{eventId}")
    public ResponseEntity<Void> deleteTeamRecurringEvent(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @PathVariable
            Long eventId
    ) {
        teamEventService.deleteTeamRecurringEvent(userDetails.getUsername(), eventId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/recurring/instance/{eventId}")
    public ResponseEntity<Void> deleteTeamRecurringInstance(
            @AuthenticationPrincipal
            UserDetails userDetails,
            @PathVariable
            Long eventId,
            @RequestBody @Valid
            RecurringInstanceDeleteRequestDto requestDto
    ) {
        teamEventService.deleteTeamRecurringInstance(
                userDetails.getUsername(),
                eventId,
                requestDto
        );

        return ResponseEntity.noContent().build();
    }
}
