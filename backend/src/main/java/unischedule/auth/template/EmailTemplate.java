package unischedule.auth.template;

public class EmailTemplate {
    public static final String AUTH_CODE_TEMPLATE = """
            <div style="text-align: center; font-family: 'Pretendard', Arial, sans-serif; padding: 40px;">
                <h2 style="color: #333;">아래 인증코드를 입력해주세요.</h2>
                <div style="font-size: 36px; font-weight: bold; margin: 30px 0; letter-spacing: 4px; color: #2C7BE5;">
                    %s
                </div>
                <p style="font-size: 16px; color: #555;">인증번호 유효시간은 5분입니다.</p>
                <p style="margin-top: 40px; color: #999;">- UniSchedule -</p>
            </div>
            """;
}
