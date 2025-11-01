package unischedule.team.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import unischedule.team.domain.WhenToMeet;

public record WhenToMeetRecommendResponseDto(
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
     * @return WhenToMeetRecommendResponseDto
     */
    public static WhenToMeetRecommendResponseDto from(WhenToMeet window, long totalMembers) {
        
        String week = window.getStartTime().format(DAY_OF_WEEK_FORMATTER);
        String status = calculateStatus(window.getAvailableMember(), totalMembers);
        
        return new WhenToMeetRecommendResponseDto(
            week,
            status,
            window.getStartTime(),
            window.getEndTime(),
            window.getAvailableMember()
        );
    }
    
    /**
     * 참여 가능 인원 비율에 따라 슬롯의 상태를 계산합니다.
     *
     * @param available    참여 가능한 인원 수
     * @param totalMembers 전체 인원 수
     * @return 상태 문자열 ("최적", "좋음", "보통", "불가능")
     */
    private static String calculateStatus(long available, long totalMembers) {
        
        if (totalMembers <= 0 || available <= 0) {
            return "불가능";
        }
        
        if (available == totalMembers) {
            return "최적";
        }
        
        double ratio = (double) available / totalMembers;
        
        if (ratio >= 0.75) {
            return "좋음";
        }
        
        else {
            return "보통";
        }
    }
}
