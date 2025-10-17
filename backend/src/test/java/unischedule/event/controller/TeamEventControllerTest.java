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
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.dto.TeamEventCreateRequestDto;
import unischedule.events.service.TeamEventService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        given(teamEventService.createTeamSingleEvent(anyString(), any(TeamEventCreateRequestDto.class))).willReturn(responseDto);

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
    @DisplayName("팀 반복 일정 생성")
    void createTeamRecurringEvent() throws Exception {
        // given
        Long teamId = 1L;
        RecurringEventCreateRequestDto requestDto = new RecurringEventCreateRequestDto(
                "팀 반복 회의", "매주",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), false, "FREQ=WEEKLY");
        EventCreateResponseDto responseDto = new EventCreateResponseDto(1L, "팀 반복 회의", null, null, null, false);
        given(teamEventService.createTeamRecurringEvent(anyString(), anyLong(), any(RecurringEventCreateRequestDto.class))).willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/events/team/recurring/add/{teamId}", teamId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("팀 반복 회의"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀의 특정 단일 일정 조회")
    void getTeamEvent() throws Exception {
        // given
        Long eventId = 1L;
        EventGetResponseDto responseDto = new EventGetResponseDto(eventId, "팀 회의", "내용",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), false, false);
        given(teamEventService.getTeamEvent(anyString(), anyLong())).willReturn(responseDto);

        // when & then
        mockMvc.perform(get("/api/events/team/event/{eventId}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event_id").value(eventId));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀의 특정 기간 일정 조회")
    void getTeamEvents() throws Exception {
        // given
        Long teamId = 1L;
        EventGetResponseDto responseDto = new EventGetResponseDto(1L, "팀 회의", "내용",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), false, false);
        given(teamEventService.getTeamEvents(anyString(), anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(List.of(responseDto));

        // when & then
        mockMvc.perform(get("/api/events/team/{teamId}", teamId)
                        .param("startAt", "2025-01-01T00:00:00")
                        .param("endAt", "2025-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("팀 회의"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 일정 수정")
    void modifyTeamEvent() throws Exception {
        // given
        Long eventId = 1L;
        EventModifyRequestDto requestDto = new EventModifyRequestDto("Updated Title", null, null, null, null);
        EventGetResponseDto responseDto = new EventGetResponseDto(
                eventId,
                "Updated Title",
                "Description",
                LocalDateTime.of(2025, 10, 1, 10, 0),
                LocalDateTime.of(2025, 10, 1, 11, 0),
                false,
                false
        );

        given(teamEventService.modifyTeamEvent(anyString(), anyLong(), any(EventModifyRequestDto.class)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(patch("/api/events/team/modify/{eventId}", eventId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 반복 일정 전체 수정")
    void modifyTeamRecurringEvent() throws Exception {
        // given
        Long eventId = 1L;
        EventModifyRequestDto requestDto = new EventModifyRequestDto("수정된 팀 반복", null, null, null, null);
        EventGetResponseDto responseDto = new EventGetResponseDto(eventId, "수정된 팀 반복", null, null, null, false, true);
        given(teamEventService.modifyTeamRecurringEvent(anyString(), anyLong(), any(EventModifyRequestDto.class))).willReturn(responseDto);

        // when & then
        mockMvc.perform(patch("/api/events/team/recurring/modify/{eventId}", eventId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 팀 반복"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 반복 일정 특정 날짜 수정")
    void modifyTeamRecurringInstance() throws Exception {
        // given
        Long eventId = 1L;
        RecurringInstanceModifyRequestDto requestDto = new RecurringInstanceModifyRequestDto(LocalDateTime.now(), "회의 날짜 변경", null, null, null, null);
        EventGetResponseDto responseDto = new EventGetResponseDto(eventId, "회의 날짜 변경", null, null, null, false, true);
        given(teamEventService.modifyTeamRecurringInstance(anyString(), anyLong(), any(RecurringInstanceModifyRequestDto.class))).willReturn(responseDto);

        // when & then
        mockMvc.perform(patch("/api/events/team/recurring/instance/{eventId}", eventId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("회의 날짜 변경"));
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

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 반복 일정 전체 삭제")
    void deleteTeamRecurringEvent() throws Exception {
        // given
        Long eventId = 1L;
        doNothing().when(teamEventService).deleteTeamRecurringEvent(anyString(), anyLong());

        // when & then
        mockMvc.perform(delete("/api/events/team/recurring/{eventId}", eventId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("팀 반복 일정 특정 날짜 삭제")
    void deleteTeamRecurringInstance() throws Exception {
        // given
        Long eventId = 1L;
        RecurringInstanceDeleteRequestDto requestDto = new RecurringInstanceDeleteRequestDto(LocalDateTime.now());
        doNothing().when(teamEventService).deleteTeamRecurringInstance(anyString(), anyLong(), any(RecurringInstanceDeleteRequestDto.class));

        // when & then
        mockMvc.perform(delete("/api/events/team/recurring/instance/{eventId}", eventId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isNoContent());
    }
}
