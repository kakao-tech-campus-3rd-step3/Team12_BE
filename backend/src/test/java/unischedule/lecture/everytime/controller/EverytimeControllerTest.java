package unischedule.lecture.everytime.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.common.config.SecurityConfig;
import unischedule.lecture.everytime.dto.TimetableDetailDto;
import unischedule.lecture.everytime.dto.TimetableDto;
import unischedule.lecture.everytime.service.EverytimeService;
import unischedule.exception.ExternalApiException;
import unischedule.exception.InvalidInputException;

import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EverytimeController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EverytimeController 테스트")
class EverytimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EverytimeService everytimeService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("시간표 목록 조회 성공")
    void getTimetables_Success() throws Exception {
        // given
        String validUrl = "https://everytime.kr/@abcdefghij1234567890";
        List<TimetableDto> expectedTimetables = List.of(
                new TimetableDto("2025", "1", "testIdentifier123456"),
                new TimetableDto("2025", "2", "testIdentifier654321")
        );

        given(everytimeService.getTimetables(validUrl))
                .willReturn(expectedTimetables);

        // when & then
        mockMvc.perform(get("/api/everytime/timetables")
                        .param("url", validUrl)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].year").value("2025"))
                .andExpect(jsonPath("$[0].semester").value("1"))
                .andExpect(jsonPath("$[0].identifier").value("testIdentifier123456"))
                .andExpect(jsonPath("$[1].year").value("2025"))
                .andExpect(jsonPath("$[1].semester").value("2"))
                .andExpect(jsonPath("$[1].identifier").value("testIdentifier654321"));
    }

    @Test
    @DisplayName("시간표 상세 조회 성공")
    void getTimetableDetail_Success() throws Exception {
        // given
        String validIdentifier = "abcdefghij1234567890";
        TimetableDetailDto expectedDetail = new TimetableDetailDto(
                "2025",
                "1",
                List.of(new TimetableDetailDto.Subject(
                        "웹응용프로그래밍",
                        "홍길동",
                        3,
                        List.of(new TimetableDetailDto.Subject.Time(
                                0,
                                LocalTime.of(9, 0),
                                LocalTime.of(10, 15),
                                "201-6515"
                        ))
                ))
        );

        given(everytimeService.getTimetableDetail(validIdentifier))
                .willReturn(expectedDetail);

        // when & then
        mockMvc.perform(get("/api/everytime/timetable")
                        .param("identifier", validIdentifier)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.year").value("2025"))
                .andExpect(jsonPath("$.semester").value("1"))
                .andExpect(jsonPath("$.subjects.length()").value(1))
                .andExpect(jsonPath("$.subjects[0].name").value("웹응용프로그래밍"))
                .andExpect(jsonPath("$.subjects[0].professor").value("홍길동"))
                .andExpect(jsonPath("$.subjects[0].credit").value(3))
                .andExpect(jsonPath("$.subjects[0].times.length()").value(1))
                .andExpect(jsonPath("$.subjects[0].times[0].dayOfWeek").value(0))
                .andExpect(jsonPath("$.subjects[0].times[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.subjects[0].times[0].endTime").value("10:15:00"))
                .andExpect(jsonPath("$.subjects[0].times[0].place").value("201-6515"));
    }

    @Test
    @DisplayName("잘못된 URL 형식으로 시간표 목록 조회 시 400 에러")
    void getTimetables_InvalidUrlFormat() throws Exception {
        // given
        String invalidUrl = "https://invalid-url.com";

        // when & then
        mockMvc.perform(get("/api/everytime/timetables")
                        .param("url", invalidUrl)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("잘못된 identifier 형식으로 시간표 상세 조회 시 400 에러")
    void getTimetableDetail_InvalidIdentifierFormat() throws Exception {
        // given
        String invalidIdentifier = "invalid-identifier";

        // when & then
        mockMvc.perform(get("/api/everytime/timetable")
                        .param("identifier", invalidIdentifier)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("외부 API 호출 실패 시 500 에러")
    void getTimetables_ExternalApiException() throws Exception {
        // given
        String validUrl = "https://everytime.kr/@abcdefghij1234567890";
        given(everytimeService.getTimetables(any()))
                .willThrow(new ExternalApiException("에브리타임 API 호출에 실패했습니다."));

        // when & then
        mockMvc.perform(get("/api/everytime/timetables")
                        .param("url", validUrl)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("에브리타임 API 호출에 실패했습니다."));
    }

    @Test
    @DisplayName("InvalidInputException 시 400 에러")
    void getTimetableDetail_InvalidInputException() throws Exception {
        // given
        String validIdentifier = "abcdefghij1234567890";
        given(everytimeService.getTimetableDetail(validIdentifier))
                .willThrow(new InvalidInputException("해당 시간표를 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/everytime/timetable")
                        .param("identifier", validIdentifier)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("해당 시간표를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("이미지로 시간표 상세 조회 성공")
    void getTimetableDetailFromImage_Success() throws Exception {
        // given
        MockMultipartFile image = new MockMultipartFile(
                "image", "timetable.png", "image/png", "image-content".getBytes());
        TimetableDetailDto expectedDetail = TimetableDetailDto.of(
                "2025", "1", List.of()
        );

        given(everytimeService.getTimetableDetailFromImage(any()))
                .willReturn(expectedDetail);

        // when & then
        mockMvc.perform(multipart("/api/everytime/timetable")
                        .file(image))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.year").value("2025"))
                .andExpect(jsonPath("$.semester").value("1"));
    }

}

