package unischedule.google.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import unischedule.google.domain.GoogleAuthToken;
import unischedule.google.repository.GoogleAuthTokenRepository;
import unischedule.google.service.GoogleCalendarService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final GoogleAuthTokenRepository googleAuthTokenRepository;
    private final MemberRawService memberRawService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final GoogleCalendarService googleCalendarService;

    @Value("${frontend.redirect.url}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

        OAuth2AuthorizedClient client = authorizedClientService
                .loadAuthorizedClient(
                        token.getAuthorizedClientRegistrationId(),
                        token.getName()
                );

        OAuth2RefreshToken googleRefreshToken = client.getRefreshToken();
        String refreshTokenString = (googleRefreshToken != null) ? googleRefreshToken.getTokenValue() : null;

        HttpSession session = request.getSession(false);
        String memberEmail = null;

        String successUrl = frontendRedirectUrl + "?google_sync=success";
        String errorUrl = frontendRedirectUrl + "?google_sync=error";

        if (session != null) {
            Object emailAttr = session.getAttribute("UNISCHEDULE_USER_EMAIL_FOR_LINKING");
            if (emailAttr != null) {
                memberEmail = (String) emailAttr;
                session.removeAttribute("UNISCHEDULE_USER_EMAIL_FOR_LINKING");
            }
        }

        try {
            if (memberEmail == null) {
                throw new IllegalStateException("연동할 UniSchedule 사용자 정보를 세션에서 찾을 수 없습니다.");
            }

            Member member = memberRawService.findMemberByEmail(memberEmail);;

            // 4. GoogleAuthToken 저장 또는 업데이트
            googleAuthTokenRepository.findByMember(member)
                    .ifPresentOrElse(
                            authToken -> {
                                authToken.updateRefreshToken(refreshTokenString);
                                googleAuthTokenRepository.save(authToken);
                                log.info("Google Refresh Token updated for user");
                            },
                            () -> {
                                if (refreshTokenString == null) {
                                    throw new IllegalStateException("Refresh Token이 발급되지 않았습니다. Google 계정에서 앱 권한 삭제 후 다시 시도해야 합니다.");
                                }
                                GoogleAuthToken newAuthToken = new GoogleAuthToken(member, refreshTokenString);
                                googleAuthTokenRepository.save(newAuthToken);
                                log.info("New Google Refresh Token saved for user");
                            }
                    );

            try {
                googleCalendarService.syncEvents(memberEmail);
                log.info("Initial calendar sync successful for user: {}", memberEmail);
                getRedirectStrategy().sendRedirect(request, response, successUrl);
            } catch (Exception syncException) {
                log.error("Failed but token was saved. User: {}, Error: {}",
                        memberEmail, syncException.getMessage());
                getRedirectStrategy().sendRedirect(request, response, errorUrl);
            }

            // 프론트 리다이렉션 페이지
            //getRedirectStrategy().sendRedirect(request, response, frontendRedirectUrl);

        } catch (Exception e) {
            log.error("Failed to process OAuth2 success", e);
            // 에러 페이지
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }
}
