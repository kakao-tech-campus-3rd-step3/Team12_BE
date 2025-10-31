package unischedule.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import unischedule.exception.EmailSendFailedException;

@Service
@RequiredArgsConstructor
public class EmailSenderService {
    private final JavaMailSender mailSender;

    /**
     * 인증 코드를 포함한 HTML 이메일을 전송합니다.
     *
     * @param to       수신자 이메일 주소
     * @param authCode 전송할 인증 코드
     */
    public void sendAuthCodeEmail(String to, String authCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("[UniSchedule] 이메일 인증 코드");

            String htmlContent = """
                    <div style="text-align: center; font-family: 'Pretendard', Arial, sans-serif; padding: 40px;">
                        <h2 style="color: #333;">아래 인증코드를 입력해주세요.</h2>
                        <div style="font-size: 36px; font-weight: bold; margin: 30px 0; letter-spacing: 4px; color: #2C7BE5;">
                            [%s]
                        </div>
                        <p style="font-size: 16px; color: #555;">인증번호 유효시간은 5분입니다.</p>
                        <p style="margin-top: 40px; color: #999;">- UniSchedule -</p>
                    </div>
                    """.formatted(authCode);

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            throw new EmailSendFailedException("이메일 전송 중 오류가 발생했습니다.");
        }
    }
}
