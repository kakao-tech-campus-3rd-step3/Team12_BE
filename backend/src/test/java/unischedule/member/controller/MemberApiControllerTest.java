package unischedule.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.member.dto.LoginRequestDto;
import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.service.MemberService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MemberApiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    @Test
    @DisplayName("회원가입 - 200")
    void signup() throws Exception {
        // given
        MemberRegistrationDto registrationDto = new MemberRegistrationDto("test12@gmail.com", "nickname", "p1a2s3s4word");
        given(memberService.isMemberExists(registrationDto.email())).willReturn(false);
        doNothing().when(memberService).registerMember(any(MemberRegistrationDto.class));

        // when & then
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("회원가입 시 이메일 중복")
    void signupDuplicated() throws Exception {
        // given
        MemberRegistrationDto registrationDto = new MemberRegistrationDto("test12@gmail.com", "nickname", "p1a2s3s4word");
        given(memberService.isMemberExists(registrationDto.email())).willReturn(true);

        // when & then
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("이미 사용중인 이메일입니다."));
    }

    @Test
    @DisplayName("로그인 - 200")
    void login() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("test12@gmail.com", "p1a2s3s4word");
        Authentication authentication = new UsernamePasswordAuthenticationToken(requestDto.email(), requestDto.password());

        AuthenticationManager mockAuthenticationManager = mock(AuthenticationManager.class);
        given(authenticationManagerBuilder.getObject()).willReturn(mockAuthenticationManager);
        given(mockAuthenticationManager.authenticate(any())).willReturn(authentication);
        given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn("test-access-token");

        // when & then
        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("test-access-token"));
    }
}