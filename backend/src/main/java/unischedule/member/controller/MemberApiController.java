package unischedule.member.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.member.dto.LoginRequestDto;
import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.dto.MemberTokenResponseDto;
import unischedule.member.entity.Member;
import unischedule.member.service.MemberService;

@Controller
@RequiredArgsConstructor
public class MemberApiController {
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MemberService memberService;

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody MemberRegistrationDto requestDto) {
        if (memberService.isMemberExists(requestDto.email())) {
            return ResponseEntity.badRequest().body("이미 사용중인 이메일입니다.");
        }

        memberService.registerMember(requestDto);

        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<MemberTokenResponseDto> login(@RequestBody LoginRequestDto requestDto) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(requestDto.email(), requestDto.password());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenProvider.createAccessToken(authentication);

        return ResponseEntity.ok(new MemberTokenResponseDto(accessToken));
    }
}
