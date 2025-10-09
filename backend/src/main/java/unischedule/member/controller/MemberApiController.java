package unischedule.member.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.auth.service.RefreshTokenService;
import unischedule.member.dto.AccessTokenRefreshRequestDto;
import unischedule.member.dto.CurrentMemberInfoResponseDto;
import unischedule.member.dto.LoginRequestDto;
import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.dto.MemberTokenResponseDto;
import unischedule.member.service.MemberService;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberApiController {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final MemberService memberService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody MemberRegistrationDto requestDto) {
        memberService.registerMember(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<MemberTokenResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(requestDto.email(), requestDto.password());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = refreshTokenService.issueRefreshToken(authentication);

        return ResponseEntity.ok(new MemberTokenResponseDto(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<MemberTokenResponseDto> refresh(@Valid @RequestBody AccessTokenRefreshRequestDto requestDto) {
        String newAccessToken = refreshTokenService.reissueAccessToken(requestDto.refreshToken());

        return ResponseEntity.ok(new MemberTokenResponseDto(newAccessToken, requestDto.refreshToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentMemberInfoResponseDto> getCurrentMemberInfo(@AuthenticationPrincipal UserDetails userDetails) {
        CurrentMemberInfoResponseDto responseDto = memberService.getCurrentMemberInfo(userDetails.getUsername());

        return ResponseEntity.ok(responseDto);
    }
}
