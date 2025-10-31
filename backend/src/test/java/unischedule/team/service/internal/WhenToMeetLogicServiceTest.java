package unischedule.team.service.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.member.domain.Member;
import unischedule.team.domain.WhenToMeet;
import unischedule.util.TestUtil;

@ExtendWith(MockitoExtension.class) // Mockito 사용 시
class WhenToMeetLogicServiceTest {
    
    // LogicService가 유일하게 의존하는 Mock 객체
    @Mock
    private WhenToMeetRawService whenToMeetRawService;
    
    @InjectMocks
    private WhenToMeetLogicService whenToMeetLogicService;
    
    @Test
    @DisplayName("generateIntervals (파라미터 O): 여러 날짜의 교집합을 정확히 계산한다")
    void generateIntervals_Parameterized_MultiDay_Partial() {
        //Given
        // 11월 1일 10:30 부터 (09:00 이후 시작)
        LocalDateTime start = LocalDateTime.of(2025, 11, 1, 10, 30);
        // 11월 2일 18:00 까지 (24:00 이전 종료)
        LocalDateTime end = LocalDateTime.of(2025, 11, 2, 18, 0);
        
        //When
        List<LocalDateTime> starts = whenToMeetLogicService.generateIntervalStarts(start, end);
        List<LocalDateTime> ends = whenToMeetLogicService.generateIntervalEnds(start, end);
        
        //Then
        
        // 총 2일치(11/1, 11/2)의 간격이 나와야 함
        assertThat(starts).hasSize(2);
        assertThat(ends).hasSize(2);
        
        // 1. 11월 1일의 간격 (10:30 ~ 24:00)
        assertThat(starts.get(0)).isEqualTo("2025-11-01T10:30:00"); // 09:00가 아닌 10:30
        assertThat(ends.get(0)).isEqualTo("2025-11-02T00:00:00"); // 24:00 (다음날 0시)
        
        // 2. 11월 2일의 간격 (09:00 ~ 18:00)
        assertThat(starts.get(1)).isEqualTo("2025-11-02T09:00:00"); // 09:00
        assertThat(ends.get(1)).isEqualTo("2025-11-02T18:00:00"); // 24:00가 아닌 18:00
    }
    
    @Test
    @DisplayName("generateIntervals (파라미터 O): 단 하루(Single Day) 범위가 정확히 계산된다")
    void generateIntervals_Parameterized_SingleDay_Partial() {
        //Given
        // 11월 1일 11:00 부터
        LocalDateTime start = LocalDateTime.of(2025, 11, 1, 11, 0);
        // 11월 1일 15:00 까지
        LocalDateTime end = LocalDateTime.of(2025, 11, 1, 15, 0);
        
        //When
        List<LocalDateTime> starts = whenToMeetLogicService.generateIntervalStarts(start, end);
        List<LocalDateTime> ends = whenToMeetLogicService.generateIntervalEnds(start, end);
        
        //Then
        assertThat(starts).hasSize(1);
        assertThat(ends).hasSize(1);
        
        assertThat(starts.get(0)).isEqualTo("2025-11-01T11:00:00");
        assertThat(ends.get(0)).isEqualTo("2025-11-01T15:00:00");
    }
    
    @Test
    @DisplayName("generateIntervals (파라미터 O): 업무 시간(09:00) 이전에 겹치는 경우 09:00로 잘라낸다")
    void generateIntervals_Parameterized_OverlapBeforeNine() {
        //Given
        // 11월 1일 08:00 부터 (업무 시간 전)
        LocalDateTime start = LocalDateTime.of(2025, 11, 1, 8, 0);
        // 11월 1일 10:00 까지
        LocalDateTime end = LocalDateTime.of(2025, 11, 1, 10, 0);
        
        //When
        List<LocalDateTime> starts = whenToMeetLogicService.generateIntervalStarts(start, end);
        List<LocalDateTime> ends = whenToMeetLogicService.generateIntervalEnds(start, end);
        
        //Then
        assertThat(starts).hasSize(1);
        assertThat(ends).hasSize(1);
        
        // 시작 시간이 08:00가 아닌 09:00여야 함 (dayStart = start.isAfter(dayStart) ? start : dayStart;)
        assertThat(starts.get(0)).isEqualTo("2025-11-01T09:00:00");
        assertThat(ends.get(0)).isEqualTo("2025-11-01T10:00:00");
    }
    
    @Test
    @DisplayName("generateIntervals (파라미터 O): 업무 시간과 전혀 겹치지 않으면 빈 리스트를 반환한다")
    void generateIntervals_Parameterized_NoOverlap() {
        //Given
        // 11월 1일 06:00 부터
        LocalDateTime start = LocalDateTime.of(2025, 11, 1, 6, 0);
        // 11월 1일 08:00 까지 (업무 시간 09:00 이전에 종료)
        LocalDateTime end = LocalDateTime.of(2025, 11, 1, 8, 0);
        
        // --- When (실행) ---
        List<LocalDateTime> starts = whenToMeetLogicService.generateIntervalStarts(start, end);
        List<LocalDateTime> ends = whenToMeetLogicService.generateIntervalEnds(start, end);
        
        // --- Then (검증) ---
        // 교집합이 없으므로 빈 리스트여야 함
        assertThat(starts).isEmpty();
        assertThat(ends).isEmpty();
    }
    
    @Test
    @DisplayName("generateIntervalStarts (파라미터 없음): '오늘'을 기준으로 7일간의 시작 시간(09:00)을 반환한다")
    void generateIntervalStarts_Default_Success() {
        
        //Given
        //'오늘' 날짜를 고정
        final LocalDate fixedToday = LocalDate.of(2025, 10, 31);
        
        try (MockedStatic<LocalDate> mockedDate = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mockedDate.when(LocalDate::now).thenReturn(fixedToday);
            
            //When
            List<LocalDateTime> starts = whenToMeetLogicService.generateIntervalStarts();
            
            //Then
            // effectiveStart = 2025-11-01T09:00
            // effectiveEnd = 2025-11-08T00:00
            // (11/1 ~ 11/7, 총 7일간)
            
            assertThat(starts).hasSize(7);
            
            // 1. 첫 번째 날짜(11월 1일)의 09:00
            assertThat(starts.get(0)).isEqualTo("2025-11-01T09:00:00");
            
            // 2. 마지막 날짜(11월 7일)의 09:00
            assertThat(starts.get(6)).isEqualTo("2025-11-07T09:00:00");
        }
    }
    
    @Test
    @DisplayName("generateIntervalEnds (파라미터 없음): '오늘'을 기준으로 7일간의 종료 시간(24:00)을 반환한다")
    void generateIntervalEnds_Default_Success() {
        
        //Given
        final LocalDate fixedToday = LocalDate.of(2025, 10, 31);
        
        try (MockedStatic<LocalDate> mockedDate = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS)) {
            mockedDate.when(LocalDate::now).thenReturn(fixedToday);
            
            //When
            List<LocalDateTime> ends = whenToMeetLogicService.generateIntervalEnds();
            
            //Then
            // (11/1 ~ 11/7, 총 7일간)
            
            assertThat(ends).hasSize(7);
            
            // 1. 첫 번째 날짜(11월 1일)의 종료 시간 (다음 날 0시)
            assertThat(ends.get(0)).isEqualTo("2025-11-02T00:00:00");
            
            // 2. 마지막 날짜(11월 7일)의 종료 시간 (다음 날 0시)
            assertThat(ends.get(6)).isEqualTo("2025-11-08T00:00:00");
        }
    }
    
    @Test
    @DisplayName("generateSlots: 1시간(9:00~10:00) 범위에 15분 단위 슬롯 4개가 생성되어야 한다")
    void generateSlots_Success() {
        //Given
        Member member1 = TestUtil.makeMember();
        Member member2 = TestUtil.makeMember();
        List<Member> members = List.of(member1, member2);
        long memberCount = members.size(); // 2명
        
        LocalDateTime start = LocalDateTime.of(2025, 11, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2025, 11, 1, 10, 0); // 1시간
        List<LocalDateTime> starts = List.of(start);
        List<LocalDateTime> ends = List.of(end);
        
        //When
        List<WhenToMeet> slots = whenToMeetLogicService.generateSlots(members, starts, ends);
        
        //Then
        assertThat(slots).hasSize(4); // 1시간 = 15분 * 4
        
        // 1번째 슬롯 검증
        assertThat(slots.get(0).getStartTime()).isEqualTo("2025-11-01T09:00:00");
        assertThat(slots.get(0).getEndTime()).isEqualTo("2025-11-01T09:15:00");
        assertThat(slots.get(0).getAvailableMember()).isEqualTo(memberCount);
        
        // 마지막(4번째) 슬롯 검증
        assertThat(slots.get(3).getStartTime()).isEqualTo("2025-11-01T09:45:00");
        assertThat(slots.get(3).getEndTime()).isEqualTo("2025-11-01T10:00:00");
        assertThat(slots.get(3).getAvailableMember()).isEqualTo(memberCount);
    }
    
    @Test
    @DisplayName("applyMemberEvents: 멤버 1명이 9:00~9:30에 일정이 있으면 해당 슬롯 2개의 카운트가 1 감소해야 한다")
    void applyMemberEvents_Success() {
        //Given
        Member member1 = TestUtil.makeMember();
        Member member2 = TestUtil.makeMember();
        List<Member> members = List.of(member1, member2); // 총 2명
        
        LocalDateTime dayStart = LocalDateTime.of(2025, 11, 1, 9, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2025, 11, 1, 10, 0);
        List<LocalDateTime> intervalStarts = List.of(dayStart);
        List<LocalDateTime> intervalEnds = List.of(dayEnd);
        
        // 1. 테스트할 슬롯 (초기값: 2명)
        List<WhenToMeet> slots = List.of(
            new WhenToMeet(dayStart, dayStart.plusMinutes(15), 2L),                   // 09:00~09:15
            new WhenToMeet(dayStart.plusMinutes(15), dayStart.plusMinutes(30), 2L), // 09:15~09:30
            new WhenToMeet(dayStart.plusMinutes(30), dayStart.plusMinutes(45), 2L)  // 09:30~09:45
        );
        
        // 2. Mock RawService 설정:
        // Alice(member1)는 9:00~9:30에 일정이 있다.
        LocalDateTime eventStart = LocalDateTime.of(2025, 11, 1, 9, 0);
        LocalDateTime eventEnd = LocalDateTime.of(2025, 11, 1, 9, 30);
        EventGetResponseDto aliceEvent = new EventGetResponseDto(1L, "title", "", eventStart, eventEnd, false, false);
        
        when(whenToMeetRawService.findMemberEvents(member1, dayStart, dayEnd)).thenReturn(List.of(aliceEvent));
        // Bob(member2)는 일정이 없다.
        when(whenToMeetRawService.findMemberEvents(member2, dayStart, dayEnd)).thenReturn(List.of());
        
        //When
        whenToMeetLogicService.applyMemberEvents(slots, members, intervalStarts, intervalEnds, whenToMeetRawService);
        
        //Then
        // Alice의 일정(9:00~9:30)과 겹치는 슬롯
        assertThat(slots.get(0).getAvailableMember()).isEqualTo(1L); // 9:00~9:15
        assertThat(slots.get(1).getAvailableMember()).isEqualTo(1L); // 9:15~9:30
        
        // 겹치지 않는 슬롯
        assertThat(slots.get(2).getAvailableMember()).isEqualTo(2L); // 9:30~9:45 (기존 2명 그대로)
    }
    
    @Test
    @DisplayName("통합 테스트: 슬롯 생성(generateSlots)부터 이벤트 적용(applyMemberEvents)까지")
    void generateAndApplyEvents_IntegrationTest_Success() {
        
        //Given
        Member member1 = TestUtil.makeMember(); // (TestUtil이 있다고 가정)
        Member member2 = TestUtil.makeMember();
        List<Member> members = List.of(member1, member2); // 총 2명
        
        // 1. generateSlots에 사용할 시간 범위 (1시간 = 15분 슬롯 4개)
        LocalDateTime dayStart = LocalDateTime.of(2025, 11, 1, 9, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2025, 11, 1, 10, 0);
        List<LocalDateTime> intervalStarts = List.of(dayStart);
        List<LocalDateTime> intervalEnds = List.of(dayEnd);
        
        // 2. applyMemberEvents에 사용할 Mock 이벤트
        // Alice(member1)는 9:00~9:30에 일정이 있다.
        LocalDateTime eventStart = LocalDateTime.of(2025, 11, 1, 9, 0);
        LocalDateTime eventEnd = LocalDateTime.of(2025, 11, 1, 9, 30);
        EventGetResponseDto aliceEvent = new EventGetResponseDto(1L, "title", "", eventStart, eventEnd, false, false);
        
        // 2-1. Mock RawService 설정
        when(whenToMeetRawService.findMemberEvents(member1, dayStart, dayEnd)).thenReturn(List.of(aliceEvent));
        when(whenToMeetRawService.findMemberEvents(member2, dayStart, dayEnd)).thenReturn(List.of());
        
        
        //When
        List<WhenToMeet> slots = whenToMeetLogicService.generateSlots(members, intervalStarts, intervalEnds);
        whenToMeetLogicService.applyMemberEvents(slots, members, intervalStarts, intervalEnds, whenToMeetRawService);
        
        
        //Then
        
        // 1. 'generateSlots'가 잘 동작했는지 검증
        // 9:00~10:00 (1시간)은 15분 단위로 총 4개의 슬롯이 생성되어야 함
        assertThat(slots).hasSize(4);
        
        // 2. 'applyMemberEvents'가 잘 동작했는지 검증
        // Alice의 일정(9:00~9:30)과 겹치는 슬롯
        assertThat(slots.get(0).getAvailableMember()).isEqualTo(1L); // 9:00~09:15 (2명 -> 1명)
        assertThat(slots.get(1).getAvailableMember()).isEqualTo(1L); // 9:15~09:30 (2명 -> 1명)
        
        // 겹치지 않는 슬롯
        assertThat(slots.get(2).getAvailableMember()).isEqualTo(2L); // 9:30~09:45 (2명 그대로)
        assertThat(slots.get(3).getAvailableMember()).isEqualTo(2L); // 9:45~10:00 (2명 그대로)
    }
}