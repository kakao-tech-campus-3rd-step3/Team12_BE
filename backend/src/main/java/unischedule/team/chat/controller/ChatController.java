package unischedule.team.chat.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import unischedule.team.chat.dto.ChatMessageHistoryRequestDto;
import unischedule.team.chat.dto.ChatMessageHistoryResponseDto;
import unischedule.team.chat.service.ChatService;

@RestController
@RequestMapping("/api/teams/{teamId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/messages")
    public ResponseEntity<ChatMessageHistoryResponseDto> getMessages(
            @PathVariable Long teamId,
            @ModelAttribute @Valid ChatMessageHistoryRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ChatMessageHistoryResponseDto response = chatService.getMessages(
                teamId, 
                userDetails.getUsername(), 
                requestDto.cursor(),
                50
        );
        return ResponseEntity.ok(response);
    }
}
