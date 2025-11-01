package unischedule.lecture.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.RecurrenceRule;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.service.internal.RecurrenceRuleRawService;
import unischedule.lecture.domain.Lecture;
import unischedule.lecture.dto.LectureResponseDto;
import unischedule.lecture.dto.LecturesCreateRequestDto;
import unischedule.lecture.dto.LecturesCreateResponseDto;
import unischedule.lecture.everytime.dto.TimetableDetailDto;
import unischedule.lecture.service.internal.LectureRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.util.TestUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LectureService 테스트")
class LectureServiceTest {

    @Mock
    private LectureRawService lectureRawService;
    @Mock
    private MemberRawService memberRawService;
    @Mock
    private CalendarRawService calendarRawService;
    @Mock
    private EventRawService eventRawService;
    @Mock
    private RecurrenceRuleRawService recurrenceRuleRawService;

    @InjectMocks
    private LectureService lectureService;

    private Member member;
    private Calendar calendar;
    private String email;

    @BeforeEach
    void setUp() {
        email = "test@test.com";
        member = spy(TestUtil.makeMember());
        calendar = spy(TestUtil.makePersonalCalendar(member));
    }

    @Test
    @DisplayName("강의 저장 성공 (첫 등록)")
    void saveLectures_Success() {
        // given
        LocalDate startDate = LocalDate.of(2025, 3, 1);
        LocalDate endDate = LocalDate.of(2025, 6, 15);
        
        TimetableDetailDto.Subject.Time time = new TimetableDetailDto.Subject.Time(
                1, LocalTime.of(9, 0), LocalTime.of(10, 30), "공학관 301"
        );
        
        TimetableDetailDto.Subject subject = new TimetableDetailDto.Subject(
                "데이터베이스", "김교수", 3, List.of(time)
        );
        
        TimetableDetailDto timetable = new TimetableDetailDto("2025", "1학기", List.of(subject));
        LecturesCreateRequestDto requestDto = new LecturesCreateRequestDto(startDate, endDate, timetable);

        RecurrenceRule recurrenceRule = new RecurrenceRule("FREQ=WEEKLY;BYDAY=MO;UNTIL=20250615T235959");
        Event event = TestUtil.makeEvent("데이터베이스", "김교수 | 공학관 301");
        
        Lecture lecture = Lecture.builder()
                .name("데이터베이스")
                .professor("김교수")
                .credit(3)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(lectureRawService.findActiveLecturesByMemberId(member.getMemberId())).willReturn(List.of());
        given(calendarRawService.getMyPersonalCalendar(member)).willReturn(calendar);
        given(recurrenceRuleRawService.saveRecurrenceRule(any(RecurrenceRule.class))).willReturn(recurrenceRule);
        given(eventRawService.saveEvent(any(Event.class))).willReturn(event);
        given(lectureRawService.saveLecture(any(Lecture.class))).willReturn(lecture);

        // when
        LecturesCreateResponseDto result = lectureService.saveLectures(email, requestDto);

        // then
        assertThat(result.lectures()).hasSize(1);
        assertThat(result.lectures().get(0).name()).isEqualTo("데이터베이스");
        assertThat(result.lectures().get(0).professor()).isEqualTo("김교수");
        
        verify(memberRawService).findMemberByEmail(email);
        verify(calendarRawService).getMyPersonalCalendar(member);
    }

    @Test
    @DisplayName("내 강의 목록 조회 성공")
    void getMyLectures_Success() {
        // given
        Event event = TestUtil.makeEvent("데이터베이스", "김교수 | 공학관 301");
        
        Lecture lecture1 = Lecture.builder()
                .name("데이터베이스")
                .professor("김교수")
                .credit(3)
                .startDate(LocalDate.of(2025, 3, 1))
                .endDate(LocalDate.of(2025, 6, 15))
                .build();
        lecture1.connectEvent(event);
        
        Lecture lecture2 = Lecture.builder()
                .name("알고리즘")
                .professor("이교수")
                .credit(3)
                .startDate(LocalDate.of(2025, 3, 1))
                .endDate(LocalDate.of(2025, 6, 15))
                .build();
        lecture2.connectEvent(event);

        given(memberRawService.findMemberByEmail(email)).willReturn(member);
        given(lectureRawService.findActiveLecturesByMemberId(member.getMemberId()))
                .willReturn(List.of(lecture1, lecture2));

        // when
        List<LectureResponseDto> result = lectureService.getMyLectures(email);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("데이터베이스");
        assertThat(result.get(1).name()).isEqualTo("알고리즘");
        
        verify(memberRawService).findMemberByEmail(email);
        verify(lectureRawService).findActiveLecturesByMemberId(member.getMemberId());
    }
}

