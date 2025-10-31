package unischedule.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unischedule.auth.dto.SendEmailRequestDto;
import unischedule.auth.dto.SendEmailResponseDto;
import unischedule.auth.dto.VerifyEmailRequestDto;
import unischedule.auth.dto.VerifyEmailResponseDto;
import unischedule.auth.service.EmailAuthService;

@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
public class EmailAuthController {
    private final EmailAuthService emailAuthService;

    @PostMapping("/send")
    public ResponseEntity<SendEmailResponseDto> sendAuthEmail(@Valid @RequestBody SendEmailRequestDto requestDto) {
        SendEmailResponseDto responseDto = emailAuthService.sendAuthEmail(requestDto.email());

        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyEmailResponseDto> verifyAuthCode(@Valid @RequestBody VerifyEmailRequestDto requestDto) {
        VerifyEmailResponseDto responseDto = emailAuthService.verifyAuthCode(requestDto.email(), requestDto.code());

        return new ResponseEntity<>(responseDto, HttpStatus.OK);
    }
}
