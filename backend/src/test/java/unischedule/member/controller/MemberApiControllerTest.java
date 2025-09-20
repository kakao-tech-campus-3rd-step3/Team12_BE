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
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.auth.service.RefreshTokenService;
import unischedule.exception.dto.EntityAlreadyExistsException;
import unischedule.member.dto.AccessTokenRefreshRequestDto;
import unischedule.member.dto.LoginRequestDto;
import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.service.MemberService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("회원가입 - 200")
    void signup() throws Exception {
        // given
        MemberRegistrationDto registrationDto = new MemberRegistrationDto("test12@gmail.com", "nickname", "p1a2s3s4word");
        doNothing().when(memberService).registerMember(any(MemberRegistrationDto.class));

        // when & then
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value("회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("회원가입 시 이메일 중복")
    void signupDuplicated() throws Exception {
        // given
        MemberRegistrationDto registrationDto = new MemberRegistrationDto("test12@gmail.com", "nickname", "p1a2s3s4word");

        doThrow(new EntityAlreadyExistsException("이미 사용중인 이메일입니다."))
                .when(memberService)
                .registerMember(any(MemberRegistrationDto.class));

        // when & then
        mockMvc.perform(post("/api/members/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 사용중인 이메일입니다."));
    }

    @Test
    @DisplayName("로그인 - 200")
    void login() throws Exception {
        // given
        LoginRequestDto requestDto = new LoginRequestDto("test12@gmail.com", "p1a2s3s4word");
        Authentication authentication = new UsernamePasswordAuthenticationToken(requestDto.email(), requestDto.password());

        //AuthenticationManager mockAuthenticationManager = mock(AuthenticationManager.class);
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(authentication);
        given(jwtTokenProvider.createAccessToken(any(Authentication.class))).willReturn("test-access-token");
        given(refreshTokenService.issueRefreshToken(any(Authentication.class))).willReturn("test-refresh-token");


        // when & then
        mockMvc.perform(post("/api/members/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("test-access-token"))
                .andExpect(jsonPath("$.refresh_token").value("test-refresh-token"));
    }

    @Test
    @DisplayName("토큰 재발급")
    void refresh() throws Exception {
        // given
        AccessTokenRefreshRequestDto requestDto = new AccessTokenRefreshRequestDto("test-refresh-token");
        given(refreshTokenService.reissueAccessToken(any(String.class))).willReturn("new-access-token");

        // when & then
        mockMvc.perform(post("/api/members/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new-access-token"))
                .andExpect(jsonPath("$.refresh_token").value("test-refresh-token"));
    }
}
