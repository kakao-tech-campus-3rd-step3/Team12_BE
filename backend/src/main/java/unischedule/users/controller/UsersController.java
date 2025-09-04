package unischedule.users.controller;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import unischedule.users.dto.EventGetRequestDto;
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
        @RequestParam LocalDateTime startAt,
        @RequestParam LocalDateTime endAt,
        @PathVariable Long userId
    ) {
        List<EventGetResponseDto> responseDto = usersService.getEvents(startAt, endAt, userId);
        return ResponseEntity.ok(responseDto);
    }
    //현재는 태그 없이 바로 리스트형태 반환
    //추후 태그 필요 시 태그를 포함하는 Dto 형태로 다시 작성할 필요 있음
}
