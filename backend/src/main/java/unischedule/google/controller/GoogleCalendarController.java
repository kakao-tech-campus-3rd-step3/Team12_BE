package unischedule.google.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unischedule.google.service.GoogleCalendarService;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/google/calendar")
@RequiredArgsConstructor
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;

    /**
     * 사용자의 구글 캘린더 일정 동기화
     * 사전에 /oauth2/authorization/google 을 통해 연동이 완료되어 있어야 함)
     */
    @PostMapping("/sync")
    public ResponseEntity<Void> syncMyCalendar(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String userEmail = userDetails.getUsername();

        try {
            googleCalendarService.syncEvents(userEmail);
            return ResponseEntity.noContent().build();
        }
        catch (IllegalStateException e) {
            URI googleAuthUri = URI.create("/oauth2/authorization/google");
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(googleAuthUri)
                    .build();
        }
    }
}
