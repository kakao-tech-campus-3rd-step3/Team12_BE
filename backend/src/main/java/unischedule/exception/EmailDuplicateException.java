package unischedule.exception;


public class EmailDuplicateException extends RuntimeException {
    public EmailDuplicateException(String email) {
        super("이미 사용 중인 이메일 주소입니다: " + email);
    }
}
