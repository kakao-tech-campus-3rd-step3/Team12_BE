package unischedule.exception;


public class EmailAuthAlreadySentException extends RuntimeException {
    public EmailAuthAlreadySentException(String email) {
        super("이미 인증 코드가 발송된 이메일입니다. 잠시 후 다시 시도해주세요: " + email);
    }
}
