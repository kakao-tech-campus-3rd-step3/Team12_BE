package unischedule.event.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.common.config.SecurityConfig;
import unischedule.events.controller.TeamEventController;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.service.TeamEventService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamEventController.class)
@Import(SecurityConfig.class)
public class TeamEventControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TeamEventService teamEventService;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 일정 추가")
    void createTeamEvent() throws Exception {
        // given
        TeamEventCreateRequestDto requestDto = new TeamEventCreateRequestDto(
                1L,
                "Team Event",
                "Description",
                LocalDateTime.of(2025, 10, 1, 10, 0),
                LocalDateTime.of(2025, 10, 1, 11, 0),
                false
        );
        EventCreateResponseDto responseDto = new EventCreateResponseDto(
                1L,
                "Team Event",
                "Description",
                requestDto.startTime(),
                requestDto.endTime(),
                false
        );

        given(teamEventService.createTeamEvent(anyString(), any(TeamEventCreateRequestDto.class))).willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/events/team/add")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.event_id").value(1L))
                .andExpect(jsonPath("$.title").value("Team Event"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 일정 수정")
    void modifyTeamEvent() throws Exception {
        // given
        Long eventId = 1L;
        EventModifyRequestDto requestDto = new EventModifyRequestDto(eventId, "Updated Title", null, null, null, null);
        EventGetResponseDto responseDto = new EventGetResponseDto(
                eventId,
                "Updated Title",
                "Description",
                LocalDateTime.of(2025, 10, 1, 10, 0),
                LocalDateTime.of(2025, 10, 1, 11, 0),
                false,
                false
        );

        given(teamEventService.modifyTeamEvent(anyString(), any(EventModifyRequestDto.class)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(patch("/api/events/team/modify")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 일정 삭제")
    void deleteTeamEvent() throws Exception {
        // given
        Long eventId = 1L;
        doNothing().when(teamEventService).deleteTeamEvent(anyString(), anyLong());
        // when & then
        mockMvc.perform(delete("/api/events/team/{eventId}", eventId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
