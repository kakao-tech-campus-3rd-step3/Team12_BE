package unischedule.google.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unischedule.google.service.GoogleCalendarService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/google/calendar")
@RequiredArgsConstructor
public class GoogleCalendarApiController {

    private final GoogleCalendarService googleCalendarService;

    /**
     * 사용자의 구글 캘린더 일정 동기화
     * 구글 토큰 정보 부재 시 리다이렉트 : /oauth2/authorization/google
     */
    @PostMapping("/sync")
    public ResponseEntity<?> syncMyCalendar(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest request
    ) {
        String userEmail = userDetails.getUsername();

        try {
            googleCalendarService.syncEvents(userEmail);
            return ResponseEntity.noContent().build();
        }
        catch (IllegalStateException e) {
            // 이메일 세션 저장
            HttpSession session = request.getSession(true);
            session.setAttribute("UNISCHEDULE_USER_EMAIL_FOR_LINKING", userEmail);

            String googleAuthUrl = "/oauth2/authorization/google";
            Map<String, String> responseBody = Map.of(
                    "message", "Google authentication required",
                    "redirect_url", googleAuthUrl
            );

            return ResponseEntity.ok(responseBody);
        }
    }
}
