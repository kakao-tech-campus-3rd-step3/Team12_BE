package unischedule.exception;

public class EmailAuthCodeMissMatchException extends RuntimeException {
    public EmailAuthCodeMissMatchException() {
        super("이메일 인증 코드가 일치하지 않습니다");
    }
}
