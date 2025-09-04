package unischedule.users.dto;

import java.time.LocalDateTime;

public record EventGetRequestDto(LocalDateTime startAt, LocalDateTime endAt, Long userId) {

}
