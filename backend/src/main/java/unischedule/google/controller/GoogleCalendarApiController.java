package unischedule.google.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unischedule.google.dto.GoogleSyncRequestDto;
import unischedule.google.service.GoogleCalendarService;

import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/google/calendar")
@RequiredArgsConstructor
public class GoogleCalendarApiController {

    private final GoogleCalendarService googleCalendarService;
    private final ObjectMapper objectMapper;

    @Value("${frontend.redirect.url}")
    private String frontendRedirectUrl;

    /**
     * 사용자의 구글 캘린더 일정 동기화
     * 구글 토큰 정보 부재 시 리다이렉트 : /oauth2/authorization/google
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncMyCalendar(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request,
            @RequestBody(required = false)
            GoogleSyncRequestDto requestDto
    ) {
        String userEmail = userDetails.getUsername();

        try {
            googleCalendarService.syncEvents(userEmail);
            return ResponseEntity.noContent().build();
        }
        catch (IllegalStateException e) {
            String redirectUrl = (requestDto != null && requestDto.redirectUrl() != null && !requestDto.redirectUrl().isBlank())
                    ? requestDto.redirectUrl()
                    : frontendRedirectUrl;

            String state = createOauthState(userEmail, redirectUrl);

            String googleAuthUrl = "/oauth2/authorization/google?state=" + state;
            Map<String, String> responseBody = Map.of(
                    "message", "Google authentication required",
                    "redirect_url", googleAuthUrl
            );

            return ResponseEntity.ok(responseBody);
        }
    }

    private String createOauthState(String email, String redirectUrl) {
        try {
            Map<String, String> stateMap = Map.of(
                    "email", email,
                    "redirectUrl", redirectUrl
            );
            String jsonState = objectMapper.writeValueAsString(stateMap);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(jsonState.getBytes());
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Failed to create OAuth state", e);
        }
    }
}
