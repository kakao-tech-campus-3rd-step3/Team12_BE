package unischedule.team.service.internal;

import java.security.SecureRandom;

public class TeamCodeGenerator {
    
    //팀 코드 길이 변환 필요 시 조정
    private static final int DEFAULT_LENGTH = 6;
    
    //특수 문자 추가 원할 시 추가 가능
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private final SecureRandom secureRandom = new SecureRandom();
    
    public String generate() {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < DEFAULT_LENGTH; i++) {
            int index = secureRandom.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        
        return sb.toString();
    }
    
}
