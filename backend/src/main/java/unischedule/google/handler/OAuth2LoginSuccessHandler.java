package unischedule.google.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import unischedule.google.domain.GoogleAuthToken;
import unischedule.google.repository.GoogleAuthTokenRepository;
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

    @Value("${frontend.redirect.url:http://localhost:3000}")
    private String frontendRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

        OAuth2AuthorizedClient client = authorizedClientService
                .loadAuthorizedClient(
                        token.getAuthorizedClientRegistrationId(),
                        token.getName()
                );

        String refreshToken = client.getRefreshToken().getTokenValue();
        String googleEmail = token.getPrincipal().getAttribute("email");

        try {

            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            if (currentAuth == null || currentAuth.getName() == null) {
                throw new IllegalStateException("현재 로그인된 UniSchedule 사용자를 찾을 수 없습니다.");
            }

            String memberEmail = currentAuth.getName();
            Member member = memberRawService.findMemberByEmail(memberEmail);;

            // 4. GoogleAuthToken 저장 또는 업데이트
            googleAuthTokenRepository.findByMember(member)
                    .ifPresentOrElse(
                            authToken -> {
                                authToken.updateRefreshToken(refreshToken);
                                googleAuthTokenRepository.save(authToken);
                                log.info("Google Refresh Token updated for user: {}", memberEmail);
                            },
                            () -> {
                                GoogleAuthToken newAuthToken = new GoogleAuthToken(member, refreshToken);
                                googleAuthTokenRepository.save(newAuthToken);
                                log.info("New Google Refresh Token saved for user: {}", memberEmail);
                            }
                    );

            // 프론트엔드 리다이렉션 페이지
            getRedirectStrategy().sendRedirect(request, response, frontendRedirectUrl);

        } catch (Exception e) {
            log.error("Failed to process OAuth2 success", e);
            // 에러 페이지
            getRedirectStrategy().sendRedirect(request, response, frontendRedirectUrl);
        }
    }
}
