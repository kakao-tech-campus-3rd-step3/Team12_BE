package unischedule.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import unischedule.auth.entity.RefreshToken;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.auth.repository.RefreshTokenRepository;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;
import unischedule.member.repository.MemberRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("리프레시 토큰 발급 - 신규")
    void issueRefreshToken_New() {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", null);
        Member member = new Member("test@example.com", "nick", "pass");
        String generatedToken = "new-refresh-token";

        given(jwtTokenProvider.createRefreshToken(auth)).willReturn(generatedToken);
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
        given(refreshTokenRepository.findByMember(any(Member.class))).willReturn(Optional.empty());

        // when
        String refreshToken = refreshTokenService.issueRefreshToken(auth);

        // then
        assertThat(refreshToken).isEqualTo(generatedToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("리프레시 토큰 업데이트")
    void reIssueRefreshToken() {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", null);
        Member member = new Member("test@example.com", "nick", "pass");
        RefreshToken existingToken = new RefreshToken(member, "old-token");
        String newGeneratedToken = "updated-refresh-token";

        given(jwtTokenProvider.createRefreshToken(auth)).willReturn(newGeneratedToken);
        given(memberRepository.findByEmail("test@example.com")).willReturn(Optional.of(member));
        given(refreshTokenRepository.findByMember(any(Member.class))).willReturn(Optional.of(existingToken));

        // when
        String refreshToken = refreshTokenService.issueRefreshToken(auth);

        // then
        assertThat(refreshToken).isEqualTo(newGeneratedToken);
        assertThat(existingToken.getToken()).isEqualTo(newGeneratedToken); // 토큰 값이 업데이트 되었는지 확인
    }

    @Test
    @DisplayName("액세스 토큰 재발급")
    void reissueAccessToken() {
        // given
        String refreshToken = "valid-refresh-token";
        Authentication auth = new UsernamePasswordAuthenticationToken("test@example.com", null);
        Member member = new Member("test@example.com", "nick", "pass");
        RefreshToken storedToken = new RefreshToken(member, refreshToken);
        String newAccessToken = "new-access-token";

        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getAuthentication(refreshToken)).willReturn(auth);
        given(refreshTokenRepository.findByToken(refreshToken)).willReturn(Optional.of(storedToken));
        given(jwtTokenProvider.createAccessToken(auth)).willReturn(newAccessToken);

        // when
        String result = refreshTokenService.reissueAccessToken(refreshToken);

        // then
        assertThat(result).isEqualTo(newAccessToken);
    }

    @Test
    @DisplayName("액세스 토큰 재발급 - 유효하지 않은 토큰")
    void reissueAccessToken_InvalidToken() {
        // given
        String refreshToken = "invalid-refresh-token";
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(false);

        // when & then
        assertThrows(InvalidInputException.class, () -> {
            refreshTokenService.reissueAccessToken(refreshToken);
        });
    }
}
