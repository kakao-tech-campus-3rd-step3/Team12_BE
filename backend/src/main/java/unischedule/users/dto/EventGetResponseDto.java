package unischedule.users.dto;

public record EventGetResponseDto(Long eventId, String title, String description, String startTime,
                                  String endTime, Boolean isPrivate, Long ownerId, Long teamId) {
    
}
