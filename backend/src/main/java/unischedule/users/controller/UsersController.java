package unischedule.users.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import unischedule.users.dto.EventCreateRequestDto;
import unischedule.users.dto.EventCreateResponseDto;
import unischedule.users.dto.EventGetResponseDto;
import unischedule.users.service.UsersService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UsersController {
    
    private final UsersService usersService;
    
    @PostMapping("/{userId}/events")
    public ResponseEntity<EventCreateResponseDto> makeEvent(
        @PathVariable Long userId,
        @RequestBody EventCreateRequestDto requestDto
    ) {
        EventCreateResponseDto responseDto = usersService.makeEvent(userId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
    
    @GetMapping("/{userId}/events")
    public ResponseEntity<List<EventGetResponseDto>> getEvent(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startAt,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endAt,
        @PathVariable Long userId
    ) {
        // 날짜 → LocalDateTime 변환
        LocalDateTime startDateTime = startAt.atStartOfDay();           // 00:00
        LocalDateTime endDateTime = endAt.atTime(LocalTime.MAX);        // 23:59:59.999999999
        
        List<EventGetResponseDto> responseDto = usersService.getEvents(startDateTime, endDateTime, userId);
        return ResponseEntity.ok(responseDto);
    }
    //현재는 태그 없이 바로 리스트형태 반환
    //추후 태그 필요 시 태그를 포함하는 Dto 형태로 다시 작성할 필요 있음
}
