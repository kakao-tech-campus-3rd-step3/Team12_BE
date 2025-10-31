package unischedule.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import unischedule.auth.dto.SendEmailResponseDto;
import unischedule.exception.EmailAuthAlreadySentException;
import unischedule.exception.EmailDuplicateException;
import unischedule.member.service.internal.MemberRawService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class EmailAuthServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private MemberRawService memberRawService;
    @Mock
    private EmailSenderService emailSenderService;
    @InjectMocks
    private EmailAuthService emailAuthService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void 정상적인_이메일_인증코드_발송_요청_시_Redis에_코드_저장_및_이메일_전송_성공() {
        // given
        String email = "test@example.com";
        when(memberRawService.existsByEmail(email)).thenReturn(false);
        when(valueOperations.get("email:auth:" + email)).thenReturn(null);

        // when
        SendEmailResponseDto response = emailAuthService.sendAuthEmail(email);

        // then
        verify(emailSenderService, times(1)).sendAuthCodeEmail(eq(email), anyString());
        verify(valueOperations, times(1))
                .set(startsWith("email:auth:"), anyString(), eq(Duration.ofMinutes(5)));
        assertThat(response).isNotNull();
    }

    @Test
    void 이미_가입된_이메일이면_에러를_던진다() {
        // given
        String email = "duplicate@example.com";
        when(memberRawService.existsByEmail(email)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> emailAuthService.sendAuthEmail(email))
                .isInstanceOf(EmailDuplicateException.class);
    }

    @Test
    void 이미_인증코드가_발송된_이메일이면_에러를_던진다() {
        // given
        String email = "sent@example.com";
        when(memberRawService.existsByEmail(email)).thenReturn(false);
        when(valueOperations.get("email:auth:" + email)).thenReturn("123456");

        // when & then
        assertThatThrownBy(() -> emailAuthService.sendAuthEmail(email))
                .isInstanceOf(EmailAuthAlreadySentException.class);
    }
}
