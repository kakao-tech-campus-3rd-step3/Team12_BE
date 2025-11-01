package unischedule.lecture.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.common.config.SecurityConfig;
import unischedule.lecture.dto.LectureCreateResponseDto;
import unischedule.lecture.dto.LectureResponseDto;
import unischedule.lecture.dto.LecturesCreateRequestDto;
import unischedule.lecture.dto.LecturesCreateResponseDto;
import unischedule.lecture.everytime.dto.TimetableDetailDto;
import unischedule.lecture.service.LectureService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LectureController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LectureController 테스트")
class LectureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LectureService lectureService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("강의 저장 성공")
    void saveLectures_Success() throws Exception {
        // given
        TimetableDetailDto.Subject.Time time = new TimetableDetailDto.Subject.Time(
                1, LocalTime.of(9, 0), LocalTime.of(10, 30), "공학관 301"
        );
        
        TimetableDetailDto.Subject subject = new TimetableDetailDto.Subject(
                "데이터베이스", "김교수", 3, List.of(time)
        );
        
        TimetableDetailDto timetable = new TimetableDetailDto("2025", "1학기", List.of(subject));
        
        LecturesCreateRequestDto requestDto = new LecturesCreateRequestDto(
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 6, 15),
                timetable
        );

        LectureCreateResponseDto responseDto = new LectureCreateResponseDto(
                1L, 101L, "데이터베이스", "김교수", 3, List.of(time)
        );

        LecturesCreateResponseDto expectedResponse = new LecturesCreateResponseDto(List.of(responseDto));

        given(lectureService.saveLectures(any(String.class), any(LecturesCreateRequestDto.class)))
                .willReturn(expectedResponse);

        // when & then
        mockMvc.perform(put("/api/lectures")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lectures.length()").value(1))
                .andExpect(jsonPath("$.lectures[0].name").value("데이터베이스"))
                .andExpect(jsonPath("$.lectures[0].professor").value("김교수"));
    }

    @Test
    @WithMockUser(username = "test@test.com")
    @DisplayName("강의 목록 조회 성공")
    void getMyLectures_Success() throws Exception {
        // given
        List<LectureResponseDto> expectedResponse = List.of(
                new LectureResponseDto(
                        1L, 101L, "데이터베이스", "김교수", 3,
                        LocalDate.of(2025, 3, 1),
                        LocalDate.of(2025, 6, 15)
                ),
                new LectureResponseDto(
                        2L, 102L, "알고리즘", "이교수", 3,
                        LocalDate.of(2025, 3, 1),
                        LocalDate.of(2025, 6, 15)
                )
        );

        given(lectureService.getMyLectures(any(String.class)))
                .willReturn(expectedResponse);

        // when & then
        mockMvc.perform(get("/api/lectures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("데이터베이스"))
                .andExpect(jsonPath("$[1].name").value("알고리즘"));
    }
}

