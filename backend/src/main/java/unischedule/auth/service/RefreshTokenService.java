package unischedule.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.auth.entity.RefreshToken;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.auth.repository.RefreshTokenRepository;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;
import unischedule.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 리프레시 토큰 발급 메서드
     * @param authentication
     * @return
     */
    @Transactional
    public String issueRefreshToken(Authentication authentication) {
        String newRefreshToken = jwtTokenProvider.createRefreshToken(authentication);

        Member member = memberRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 계정입니다"));

        // 기존에 저장된 리프레시 토큰 있으면 갱신, 없으면 새로 저장
        refreshTokenRepository.findByMember(member)
                .ifPresentOrElse(
                        refreshToken -> refreshToken.updateToken(newRefreshToken),
                        () -> refreshTokenRepository.save(new RefreshToken(member, newRefreshToken))
                );

        return newRefreshToken;
    }

    /**
     * 액세스 토큰 재발급 메서드
     * @param refreshToken
     * @return
     */
    @Transactional(readOnly = true)
    public String reissueAccessToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidInputException("리프레시 토큰이 유효하지 않습니다.");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
        RefreshToken foundRefreshToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new EntityNotFoundException("해당 리프레시 토큰을 찾을 수 없습니다."));

        if (!foundRefreshToken.getToken().equals(refreshToken)) {
            throw new InvalidInputException("저장된 리프레시 토큰이 아닙니다.");
        }

        return jwtTokenProvider.createAccessToken(authentication);
    }
}
