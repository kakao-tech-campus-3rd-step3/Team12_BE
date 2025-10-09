package unischedule.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import unischedule.member.domain.Member;

public record CurrentMemberInfoResponseDto(
        @JsonProperty("user_id")
        Long userId,
        String name,
        String email
) {
    public static CurrentMemberInfoResponseDto from(Member member) {
        return new CurrentMemberInfoResponseDto(
                member.getMemberId(),
                member.getNickname(),
                member.getEmail()
        );
    }
}
