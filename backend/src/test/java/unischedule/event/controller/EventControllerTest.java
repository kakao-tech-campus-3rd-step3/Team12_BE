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
import unischedule.events.controller.EventController;
import unischedule.events.dto.EventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.service.EventService;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
public class EventControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private EventService eventService;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("개인 일정 추가")
    void makeMyEvent() throws Exception {
        // given
        EventCreateRequestDto requestDto = new EventCreateRequestDto(
                1L,
                "Test Event",
                "Description",
                LocalDateTime.of(2025, 9, 18, 10, 0),
                LocalDateTime.of(2025, 9, 18, 11, 0),
                false
        );
        EventCreateResponseDto responseDto = new EventCreateResponseDto(
                1L,
                "Test Event",
                "Description",
                requestDto.startTime(),
                requestDto.endTime(),
                false
        );

        given(eventService.makePersonalEvent(anyString(), any(EventCreateRequestDto.class))).willReturn(responseDto);

        // when & then
        mockMvc.perform(post("/api/events/add")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.event_id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Event"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("개인 일정 조회")
    void getMyEvents() throws Exception {
        // given
        EventGetResponseDto responseDto = new EventGetResponseDto(
                1L,
                "Test Event",
                "Description",
                LocalDateTime.of(2025, 9, 18, 10, 0),
                LocalDateTime.of(2025, 9, 18, 11, 0),
                false
        );

        given(eventService.getPersonalEvents(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(Collections.singletonList(responseDto));

        // when & then
        mockMvc.perform(get("/api/events")
                .param("startAt", "2025-09-01")
                .param("endAt", "2025-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].event_id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Event"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("개인 일정 수정")
    void modifyMyEvent() throws Exception {
        // given
        Long eventId = 1L;
        EventModifyRequestDto requestDto = new EventModifyRequestDto(1L, "Updated Title", null, null, null, null);
        EventGetResponseDto responseDto = new EventGetResponseDto(
                eventId,
                "Updated Title",
                "Description",
                LocalDateTime.of(2025, 9, 18, 10, 0),
                LocalDateTime.of(2025, 9, 18, 11, 0),
                false
        );

        given(eventService.modifyPersonalEvent(anyString(), any(EventModifyRequestDto.class)))
                .willReturn(responseDto);

        // when & then
        mockMvc.perform(patch("/api/events/modify")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    @DisplayName("개인 일정 삭제")
    void deleteMyEvent() throws Exception {
        // given
        Long eventId = 1L;
        doNothing().when(eventService).deletePersonalEvent(anyString(), anyLong());
        // when & then
        mockMvc.perform(delete("/api/events/{eventId}", eventId)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
