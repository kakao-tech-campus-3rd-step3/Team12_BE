package unischedule.everytime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import unischedule.external.EverytimeClient;
import unischedule.everytime.dto.TimetableDetailDto;
import unischedule.everytime.dto.TimetableDto;
import unischedule.external.dto.EverytimeTimetableRawResponseDto;
import unischedule.external.dto.EverytimeTimetableRawResponseDto.Attr;
import unischedule.external.dto.EverytimeTimetableRawResponseDto.PrimaryTable;
import unischedule.external.dto.EverytimeTimetableRawResponseDto.PrimaryTables;
import unischedule.external.dto.EverytimeTimetableRawResponseDto.Subject;
import unischedule.external.dto.EverytimeTimetableRawResponseDto.Table;
import unischedule.external.dto.EverytimeTimetableRawResponseDto.Time;
import unischedule.external.dto.EverytimeTimetableRawResponseDto.TimeData;
import unischedule.everytime.mapper.EverytimeTimetableMapper;
import unischedule.exception.ExternalApiException;
import unischedule.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("EverytimeService 테스트")
class EverytimeServiceTest {

    @Mock
    private EverytimeClient everytimeClient;

    @Mock
    private EverytimeTimetableMapper everytimeTimetableMapper;

    @InjectMocks
    private EverytimeService everytimeService;

    @Test
    @DisplayName("시간표 목록 조회 성공")
    void getTimetables_Success() {
        // given
        String url = "https://everytime.kr/@testIdentifier123456";
        String identifier = "testIdentifier123456";
        EverytimeTimetableRawResponseDto mockResponse = createMockRawResponse();
        List<TimetableDto> expectedTimetables = List.of(
                new TimetableDto("2025", "1", "testIdentifier123456"),
                new TimetableDto("2025", "2", "testIdentifier654321")
        );

        given(everytimeClient.fetchTimetable(identifier))
                .willReturn(Mono.just(mockResponse));
        given(everytimeTimetableMapper.toTimetableDtos(mockResponse))
                .willReturn(expectedTimetables);

        // when
        List<TimetableDto> result = everytimeService.getTimetables(url);

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedTimetables);
        verify(everytimeClient).fetchTimetable(identifier);
        verify(everytimeTimetableMapper).toTimetableDtos(mockResponse);
    }

    @Test
    @DisplayName("시간표 상세 조회 성공")
    void getTimetableDetail_Success() {
        // given
        String identifier = "testIdentifier123456";
        EverytimeTimetableRawResponseDto mockResponse = createMockRawResponse();
        TimetableDetailDto expectedDetail = createExpectedTimetableDetailDto();

        given(everytimeClient.fetchTimetable(identifier))
                .willReturn(Mono.just(mockResponse));
        given(everytimeTimetableMapper.toTimetableDetailDto(mockResponse))
                .willReturn(expectedDetail);

        // when
        TimetableDetailDto result = everytimeService.getTimetableDetail(identifier);

        // then
        assertThat(result).isEqualTo(expectedDetail);
        verify(everytimeClient).fetchTimetable(identifier);
        verify(everytimeTimetableMapper).toTimetableDetailDto(mockResponse);
    }

    @Test
    @DisplayName("외부 API 호출 실패 시 ExternalApiException 발생")
    void getTimetables_ExternalApiFailure() {
        // given
        String url = "https://everytime.kr/@testIdentifier123";
        String identifier = "testIdentifier123";
        given(everytimeClient.fetchTimetable(identifier))
                .willReturn(Mono.empty());

        // when & then
        assertThatThrownBy(() -> everytimeService.getTimetables(url))
                .isInstanceOf(ExternalApiException.class)
                .hasMessage("에브리타임 API 호출에 실패했습니다.");
    }

    @Test
    @DisplayName("시간표 상세 조회 실패 시 EntityNotFoundException 발생")
    void getTimetableDetail_EntityNotFound() {
        // given
        String identifier = "testIdentifier123";
        EverytimeTimetableRawResponseDto emptyResponse = new EverytimeTimetableRawResponseDto(
                null, null, null);
        
        given(everytimeClient.fetchTimetable(identifier))
                .willReturn(Mono.just(emptyResponse));

        // when & then
        assertThatThrownBy(() -> everytimeService.getTimetableDetail(identifier))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("해당 시간표를 찾을 수 없습니다.");
    }



    private EverytimeTimetableRawResponseDto createMockRawResponse() {
        return new EverytimeTimetableRawResponseDto(
                createMockTable(),
                null,
                createMockPrimaryTables()
        );
    }

    private Table createMockTable() {
        TimeData timeData = new TimeData(0, 0, 15, "201-6515"); // 월요일 09:00~10:15
        Time time = new Time("화 10:30(100) 201-6408&lt;br/>목 10:30(100) 201-6408",
                List.of(timeData));

        Subject subject = new Subject(
                "CB2001105",
                new Attr("CB1000119-061"),
                new Attr("웹응용프로그래밍"),
                new Attr("홍길동"),
                time,
                new Attr("201-6515"),
                new Attr("3"),
                new Attr("false")
        );

        return new Table("2025", "1", "1", "testTable123456", List.of(subject));
    }

    private PrimaryTables createMockPrimaryTables() {
        List<PrimaryTable> primaryTableList = List.of(
                new PrimaryTable("2025", "1", "testIdentifier123456"),
                new PrimaryTable("2025", "2", "testIdentifier654321")
        );
        return new PrimaryTables(primaryTableList);
    }

    private TimetableDetailDto createExpectedTimetableDetailDto() {
        TimetableDetailDto.Subject.Time time = TimetableDetailDto.Subject.Time.from(
                0,
                LocalTime.of(9, 0),
                LocalTime.of(10, 15),
                "201-6515"
        );

        TimetableDetailDto.Subject subject = TimetableDetailDto.Subject.from(
                "웹응용프로그래밍",
                "홍길동",
                3,
                List.of(time)
        );

        return TimetableDetailDto.of("2025", "1", List.of(subject));
    }
}
