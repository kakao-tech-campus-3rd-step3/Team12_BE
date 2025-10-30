package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import unischedule.team.domain.WhenToMeet;

public record WhenToMeetRecommendDto(
    String week,
    String status,
    @JsonProperty("start_time")
    LocalDateTime startTime,
    @JsonProperty("end_time")
    LocalDateTime endTime,
    Long available
) {
    // 한국어 요일 포맷터
    private static final DateTimeFormatter DAY_OF_WEEK_FORMATTER =
        DateTimeFormatter.ofPattern("E", Locale.KOREAN);
    
    /**
     * SlotWindow와 총 멤버 수를 기반으로 추천 DTO를 생성합니다.
     *
     * @param window 추천된 시간대 (시작, 끝, 참여 가능 인원)
     * @param totalMembers 팀의 총 인원
     * @return WhenToMeetRecommendDto
     */
    public static WhenToMeetRecommendDto from(WhenToMeet window, long totalMembers) {
        
        String week = window.getStartTime().format(DAY_OF_WEEK_FORMATTER);
        String status = calculateStatus(window.getAvailableMember(), totalMembers);
        
        return new WhenToMeetRecommendDto(
            week,
            status,
            window.getStartTime(),
            window.getEndTime(),
            window.getAvailableMember()
        );
    }
    
    private static String calculateStatus(long available, long totalMembers) {
        // 0명 불참
        if (available == totalMembers) {
            return "최적";
        }
        // 1명 불참
        else if (available == totalMembers - 1) {
            return "좋음";
        }
        // 2명 불참
        else if (available == totalMembers - 2) {
            return "보통";
        }
        // 3명 이상 불참 (단, 1명이라도 가능한 경우)
        else if (available > 0) {
            return "나쁨";
        }
        // 0명 이하 (아무도 참여 불가능한 경우)
        else {
            return "불가능";
        }
    }
}
