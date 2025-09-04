package unischedule.users.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
        @RequestBody EventCreateRequestDto requestDto
    ) {
        EventCreateResponseDto responseDto = usersService.makeEvent(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
    
    @GetMapping("/{userId}/events")
    public ResponseEntity<EventGetResponseDto> getEvent(
        @RequestBody EventGetRequestDto requestDto
    ) {
        EventGetResponseDto responseDto = usersService.getEvent(requestDto);
        return ResponseEntity.ok(responseDto);
    }
}
