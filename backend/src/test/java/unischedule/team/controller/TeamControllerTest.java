package unischedule.team.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.common.config.SecurityConfig;
import unischedule.google.handler.OAuth2LoginSuccessHandler;
import unischedule.team.dto.TeamCreateRequestDto;
import unischedule.team.dto.TeamCreateResponseDto;
import unischedule.team.dto.TeamJoinRequestDto;
import unischedule.team.dto.TeamJoinResponseDto;
import unischedule.team.service.TeamService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@Import(SecurityConfig.class)
public class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TeamService teamService;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private OAuth2AuthorizedClientService authorizedClientService;
    @MockitoBean
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;


    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 생성")
    void createTeam() throws Exception {
        // given
        TeamCreateRequestDto requestDto = new TeamCreateRequestDto("Test Team", "Description");
        TeamCreateResponseDto responseDto = new TeamCreateResponseDto(1L, "Test Team", "Description", "ABCDEF");
        given(teamService.createTeam(anyString(), any(TeamCreateRequestDto.class))).willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/teams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.team_name").value("Test Team"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 가입")
    void joinTeam() throws Exception {
        // given
        TeamJoinRequestDto requestDto = new TeamJoinRequestDto("ABCDEF");
        TeamJoinResponseDto responseDto = new TeamJoinResponseDto(1L, "Test Team", "Description");
        given(teamService.joinTeam(anyString(), any(TeamJoinRequestDto.class))).willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/teams/join")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.team_name").value("Test Team"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 탈퇴")
    void withdrawTeam() throws Exception {
        // given
        doNothing().when(teamService).withdrawTeam(anyString(), anyLong());

        // when & then
        mockMvc.perform(delete("/api/teams/{teamId}/member", 1L)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 폐쇄")
    void closeTeam() throws Exception {
        // given
        doNothing().when(teamService).closeTeam(anyString(), anyLong());

        // when & then
        mockMvc.perform(delete("/api/teams/{teamId}/team", 1L)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
