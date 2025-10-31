package unischedule.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import unischedule.auth.dto.SendEmailResponseDto;
import unischedule.auth.dto.VerifyEmailResponseDto;
import unischedule.exception.EmailAuthAlreadySentException;
import unischedule.exception.EmailAuthCodeMismatchException;
import unischedule.exception.EmailDuplicateException;
import unischedule.member.service.internal.MemberRawService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailAuthService {
    private static final int EMAIL_AUTH_TIMEOUT_MINUTES = 5;
    private static final String REDIS_EMAIL_AUTH_KEY_PREFIX = "email:auth:";
    private final StringRedisTemplate srt;
    private final MemberRawService memberRawService;
    private final EmailSenderService emailSenderService;

    /**
     * 인증 코드를 생성하여 이메일로 발송하고, Redis에 저장합니다.
     *
     * @param email 인증 코드를 발송할 이메일 주소
     * @return 인증 코드 만료 시간 정보를 담은 응답 DTO
     * @throws EmailDuplicateException       이미 가입된 이메일인 경우
     * @throws EmailAuthAlreadySentException 이미 인증 코드가 발송된 이메일인 경우
     */
    @Transactional
    public SendEmailResponseDto sendAuthEmail(String email) {
        validateEmail(email);
        String redisKey = REDIS_EMAIL_AUTH_KEY_PREFIX + email;
        String redisValue = generateAuthCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EMAIL_AUTH_TIMEOUT_MINUTES);
        emailSenderService.sendAuthCodeEmail(email, redisValue);
        srt.opsForValue().set(redisKey, redisValue, Duration.ofMinutes(EMAIL_AUTH_TIMEOUT_MINUTES));

        return new SendEmailResponseDto(expiresAt);
    }

    /**
     * Redis에 저장된 인증 코드와 사용자가 입력한 인증 코드를 비교하여 검증합니다.
     *
     * @param email 인증 코드를 발송한 이메일 주소
     * @param code  사용자가 입력한 인증 코드
     * @return 인증 코드 검증 결과를 담은 응답 DTO
     */
    @Transactional
    public VerifyEmailResponseDto verifyAuthCode(String email, String code) {
        String redisKey = REDIS_EMAIL_AUTH_KEY_PREFIX + email;
        String redisValue = srt.opsForValue().get(redisKey);

        if (redisValue == null || !redisValue.equals(code)) {
            throw new EmailAuthCodeMismatchException();
        }

        return new VerifyEmailResponseDto(true);
    }

    /**
     * 이메일 중복 여부와 이미 인증 코드가 발송된 이메일인지 검증합니다.
     *
     * @param email 인증 코드를 발송할 이메일 주소
     * @throws EmailDuplicateException 이미 가입된 이메일인 경우
     * @throws ResponseStatusException 이미 인증 코드가 발송된 이메일인 경우
     */
    private void validateEmail(String email) {
        if (memberRawService.existsByEmail(email)) {
            throw new EmailDuplicateException(email);
        }

        if (srt.opsForValue().get(REDIS_EMAIL_AUTH_KEY_PREFIX + email) != null) {
            throw new EmailAuthAlreadySentException(email);
        }
    }

    /**
     * 6자리 인증 코드를 생성합니다.
     *
     * @return 6자리 인증 코드 문자열
     */
    private String generateAuthCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }

        return sb.toString();
    }
}
