package unischedule.team.service.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import unischedule.team.dto.WhenToMeetRecommendResponseDto;
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
    @DisplayName("generateSlotsV2: 1시간(9:00~10:00) 범위에 30분 단위 슬롯 2개가 생성되어야 한다")
    void generateSlotsV2_Success() {
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
        List<WhenToMeet> slots = whenToMeetLogicService.generateSlotsV2(members, 30L, starts, ends);
        
        //Then
        assertThat(slots).hasSize(2); // 1시간 = 30분 * 2
        
        // 1번째 슬롯 검증
        assertThat(slots.get(0).getStartTime()).isEqualTo("2025-11-01T09:00:00");
        assertThat(slots.get(0).getEndTime()).isEqualTo("2025-11-01T09:30:00");
        assertThat(slots.get(0).getAvailableMember()).isEqualTo(memberCount);
        
        // 마지막(4번째) 슬롯 검증
        assertThat(slots.get(1).getStartTime()).isEqualTo("2025-11-01T09:30:00");
        assertThat(slots.get(1).getEndTime()).isEqualTo("2025-11-01T10:00:00");
        assertThat(slots.get(1).getAvailableMember()).isEqualTo(memberCount);
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
        EventGetResponseDto aliceEvent = new EventGetResponseDto(1L, "title", "", eventStart, eventEnd, false);
        
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
    @DisplayName("applyMemberEvents: 멤버 1명이 겹치는 일정 2개(9:00~9:30, 9:20~9:50)를 가져도, 겹치는 슬롯은 1씩만 감소해야 한다")
    void applyMemberEvents_WithOverlappingEvents_Success() {
        //Given
        Member member1 = TestUtil.makeMember(); // Alice
        Member member2 = TestUtil.makeMember(); // Bob
        List<Member> members = List.of(member1, member2); // 총 2명
        
        LocalDateTime dayStart = LocalDateTime.of(2025, 11, 1, 9, 0);
        // 9:50 일정을 포함하기 위해 dayEnd를 10:00 이후로 설정
        LocalDateTime dayEnd = LocalDateTime.of(2025, 11, 1, 10, 30);
        List<LocalDateTime> intervalStarts = List.of(dayStart);
        List<LocalDateTime> intervalEnds = List.of(dayEnd);
        
        // 1. 테스트할 슬롯 (초기값: 2명) - 9:00 ~ 10:15 범위
        List<WhenToMeet> slots = List.of(
            new WhenToMeet(dayStart, dayStart.plusMinutes(15), 2L),                   // 09:00~09:15
            new WhenToMeet(dayStart.plusMinutes(15), dayStart.plusMinutes(30), 2L), // 09:15~09:30
            new WhenToMeet(dayStart.plusMinutes(30), dayStart.plusMinutes(45), 2L), // 09:30~09:45
            new WhenToMeet(dayStart.plusMinutes(45), dayStart.plusMinutes(60), 2L), // 09:45~10:00
            new WhenToMeet(dayStart.plusMinutes(60), dayStart.plusMinutes(75), 2L)  // 10:00~10:15
        );
        
        // 2. Mock RawService 설정:
        // Alice(member1)는 겹치는 일정 2개가 있다.
        LocalDateTime eventA_Start = LocalDateTime.of(2025, 11, 1, 9, 0);  // 9:00
        LocalDateTime eventA_End = LocalDateTime.of(2025, 11, 1, 9, 30);    // 9:30
        EventGetResponseDto aliceEventA = new EventGetResponseDto(1L, "일정 A", "", eventA_Start, eventA_End, false);
        
        LocalDateTime eventB_Start = LocalDateTime.of(2025, 11, 1, 9, 20); // 9:20
        LocalDateTime eventB_End = LocalDateTime.of(2025, 11, 1, 9, 50);    // 9:50
        EventGetResponseDto aliceEventB = new EventGetResponseDto(2L, "일정 B", "", eventB_Start, eventB_End, false);
        
        // Alice는 두 일정을 모두 반환받는다.
        when(whenToMeetRawService.findMemberEvents(member1, dayStart, dayEnd))
            .thenReturn(List.of(aliceEventA, aliceEventB));
        
        // Bob(member2)는 일정이 없다.
        when(whenToMeetRawService.findMemberEvents(member2, dayStart, dayEnd)).thenReturn(List.of());
        
        //When
        whenToMeetLogicService.applyMemberEvents(slots, members, intervalStarts, intervalEnds, whenToMeetRawService);
        
        //Then
        // Alice의 "합쳐진" 일정(9:00~9:50)과 겹치는 슬롯은 모두 1이 되어야 한다.
        
        // 09:00~09:15 (일정 A와 겹침)
        assertThat(slots.get(0).getAvailableMember()).isEqualTo(1L);
        // 09:15~09:30 (일정 A, B 모두와 겹침) -> 2번 차감되지 않고 1이 되어야 함
        assertThat(slots.get(1).getAvailableMember()).isEqualTo(1L);
        // 09:30~09:45 (일정 B와 겹침)
        assertThat(slots.get(2).getAvailableMember()).isEqualTo(1L);
        // 09:45~10:00 (일정 B와 겹침. B는 9:50에 끝남)
        assertThat(slots.get(3).getAvailableMember()).isEqualTo(1L);
        
        // 10:00~10:15 (겹치지 않는 슬롯)
        assertThat(slots.get(4).getAvailableMember()).isEqualTo(2L); // 기존 2명 그대로
    }
    
    @Test
    @DisplayName("applyMemberEvents: [30분 슬롯] 2명의 멤버가 각각 다른 슬롯에 일정이 있을 때, 해당 슬롯만 1씩 감소해야 한다")
    void applyMemberEvents_With30MinSlots_Success() {
        // GIVEN
        Member member1 = TestUtil.makeMember(); // Alice
        Member member2 = TestUtil.makeMember(); // Bob
        List<Member> members = List.of(member1, member2); // 총 2명
        
        LocalDateTime dayStart = LocalDateTime.of(2025, 11, 1, 9, 0);
        LocalDateTime dayEnd = LocalDateTime.of(2025, 11, 1, 10, 30);
        List<LocalDateTime> intervalStarts = List.of(dayStart);
        List<LocalDateTime> intervalEnds = List.of(dayEnd);
        
        // 1. [30분 단위 슬롯] 생성 (초기값: 2명)
        List<WhenToMeet> slots = List.of(
            new WhenToMeet(dayStart, dayStart.plusMinutes(30), 2L),                   // 09:00~09:30 (Slot 0)
            new WhenToMeet(dayStart.plusMinutes(30), dayStart.plusMinutes(60), 2L), // 09:30~10:00 (Slot 1)
            new WhenToMeet(dayStart.plusMinutes(60), dayStart.plusMinutes(90), 2L)  // 10:00~10:30 (Slot 2)
        );
        
        // 2. Mock RawService 설정
        
        // Alice(member1)는 09:15 ~ 09:45에 일정이 있다. (Slot 0, 1에 걸침)
        LocalDateTime eventA_Start = LocalDateTime.of(2025, 11, 1, 9, 15);
        LocalDateTime eventA_End = LocalDateTime.of(2025, 11, 1, 9, 45);
        EventGetResponseDto aliceEvent = new EventGetResponseDto(1L, "Event A", "", eventA_Start, eventA_End, false);
        
        when(whenToMeetRawService.findMemberEvents(member1, dayStart, dayEnd))
            .thenReturn(List.of(aliceEvent));
        
        // Bob(member2)은 10:10 ~ 10:20에 일정이 있다. (Slot 2에 포함됨)
        LocalDateTime eventB_Start = LocalDateTime.of(2025, 11, 1, 10, 10);
        LocalDateTime eventB_End = LocalDateTime.of(2025, 11, 1, 10, 20);
        EventGetResponseDto bobEvent = new EventGetResponseDto(2L, "Event B", "", eventB_Start, eventB_End, false);
        
        when(whenToMeetRawService.findMemberEvents(member2, dayStart, dayEnd))
            .thenReturn(List.of(bobEvent));
        
        // WHEN
        // 'isMemberBusyForSlot' 헬퍼 메서드의 로직을 테스트하기 위해
        // SUT(whenToMeetLogicService)의 'applyMemberEvents' 메서드를 직접 호출
        whenToMeetLogicService.applyMemberEvents(slots, members, intervalStarts, intervalEnds, whenToMeetRawService);
        
        // THEN
        // (isMemberBusyForSlot이 겹침을 올바르게 판단했다고 가정)
        
        // Slot 0 (09:00-09:30): Alice의 9:15-9:45 일정과 겹침
        assertThat(slots.get(0).getAvailableMember()).isEqualTo(1L);
        
        // Slot 1 (09:30-10:00): Alice의 9:15-9:45 일정과 겹침
        assertThat(slots.get(1).getAvailableMember()).isEqualTo(1L);
        
        // Slot 2 (10:00-10:30): Bob의 10:10-10:20 일정과 겹침
        assertThat(slots.get(2).getAvailableMember()).isEqualTo(1L);
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
        EventGetResponseDto aliceEvent = new EventGetResponseDto(1L, "title", "", eventStart, eventEnd, false);
        
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
    
    @Test
    @DisplayName("recommendBestSlots: 60분(4슬롯) 요청 시, 최적(2명) 2개, 보통(1명) 1개를 순서대로 반환한다")
    void recommendBestSlots_Success_Top3() {
        // GIVEN
        Long durationMinutes = 60L; // 4 슬롯
        Long topN = 3L;
        Long memberCnt = 2L; // 총 멤버 2명
        
        LocalDateTime day = LocalDateTime.of(2025, 10, 30, 9, 0); // 목요일
        
        // [시나리오]
        // 09:00~10:00 (4슬롯) : [2, 2, 2, 2] -> 윈도우 (min: 2)
        // 10:00~11:00 (4슬롯) : [1, 1, 1, 1] -> 윈도우 (min: 1)
        // 11:00~12:00 (4슬롯) : [2, 2, 2, 2] -> 윈도우 (min: 2)
        // 중간 윈도우 (e.g., 09:15~10:15) : [2, 2, 2, 1] -> 윈도우 (min: 1)
        
        List<WhenToMeet> inputSlots = new ArrayList<>();
        // 09:00 ~ 10:00 (슬롯 0,1,2,3) - 2명
        for (int i = 0; i < 4; i++) {
            inputSlots.add(new WhenToMeet(day.plusMinutes(i * 15), day.plusMinutes((i + 1) * 15), 2L));
        }
        // 10:00 ~ 11:00 (슬롯 4,5,6,7) - 1명
        for (int i = 4; i < 8; i++) {
            inputSlots.add(new WhenToMeet(day.plusMinutes(i * 15), day.plusMinutes((i + 1) * 15), 1L));
        }
        // 11:00 ~ 12:00 (슬롯 8,9,10,11) - 2명
        for (int i = 8; i < 12; i++) {
            inputSlots.add(new WhenToMeet(day.plusMinutes(i * 15), day.plusMinutes((i + 1) * 15), 2L));
        }
        
        // [정렬 순서 예상]
        // 1. 09:00~10:00 (available=2, startTime=09:00)
        // 2. 11:00~12:00 (available=2, startTime=11:00)
        // 3. 09:15~10:15 (available=1, startTime=09:15)
        
        // WHEN
        List<WhenToMeetRecommendResponseDto> result = whenToMeetLogicService.recommendBestSlots(
            inputSlots, durationMinutes, topN, memberCnt
        );
        
        // THEN
        assertThat(result).hasSize(3);
        
        // 1순위 검증
        WhenToMeetRecommendResponseDto top1 = result.get(0);
        assertThat(top1.available()).isEqualTo(2L);
        assertThat(top1.status()).isEqualTo("최적");
        assertThat(top1.week()).isEqualTo("목");
        assertThat(top1.startTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 9, 0));
        assertThat(top1.endTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 10, 0));
        
        // 2순위 검증
        WhenToMeetRecommendResponseDto top2 = result.get(1);
        assertThat(top2.available()).isEqualTo(2L);
        assertThat(top2.status()).isEqualTo("최적");
        assertThat(top2.week()).isEqualTo("목");
        assertThat(top2.startTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 11, 0));
        assertThat(top2.endTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 12, 0));
        
        // 3순위 검증
        WhenToMeetRecommendResponseDto top3 = result.get(2);
        assertThat(top3.available()).isEqualTo(1L);
        assertThat(top3.status()).isEqualTo("보통");
        assertThat(top3.week()).isEqualTo("목");
        assertThat(top3.startTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 9, 15));
        assertThat(top3.endTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 10, 15));
    }
    
    @Test
    @DisplayName("recommendBestSlots: 날짜가 바뀌는 경계(자정)를 윈도우로 묶지 않아야 한다")
    void recommendBestSlots_ShouldNotCrossDateBoundary() {
        // GIVEN
        Long durationMinutes = 60L; // 4 슬롯
        Long topN = 3L;
        Long memberCnt = 2L;
        
        LocalDateTime day1 = LocalDateTime.of(2025, 11, 1, 23, 30); // 토
        LocalDateTime day2 = LocalDateTime.of(2025, 11, 2, 0, 0);  // 일
        
        // 4슬롯(60분)을 만들 수 있지만, 날짜가 갈라지는 시나리오
        List<WhenToMeet> inputSlots = List.of(
            // Day 1 (토) - 2슬롯
            new WhenToMeet(day1, day1.plusMinutes(15), 2L), // 23:30
            new WhenToMeet(day1.plusMinutes(15), day2, 2L), // 23:45
            // Day 2 (일) - 2슬롯
            new WhenToMeet(day2, day2.plusMinutes(15), 2L), // 00:00
            new WhenToMeet(day2.plusMinutes(15), day2.plusMinutes(30), 2L)  // 00:15
        );
        
        // WHEN
        // `slotsByDate`로 인해 Day1(2슬롯), Day2(2슬롯)으로 나뉨
        // 4슬롯(requiredSlots=4)을 만족하는 윈도우는 어느 날짜에도 없음
        List<WhenToMeetRecommendResponseDto> result = whenToMeetLogicService.recommendBestSlots(
            inputSlots, durationMinutes, topN, memberCnt
        );
        
        // THEN
        assertThat(result).isNotNull();
        assertThat(result).isEmpty(); // 4슬롯 윈도우가 없으므로 비어있어야 함
    }
    
    @Test
    @DisplayName("recommendBestSlots: 여러 날짜 중 참여율이 가장 높은 날짜/시간을 1순위로 반환한다")
    void recommendBestSlots_MultiDaySorting() {
        // GIVEN
        Long durationMinutes = 30L; // 2 슬롯
        Long topN = 3L;
        Long memberCnt = 3L; // 총 3명
        
        LocalDateTime day1 = LocalDateTime.of(2025, 11, 1, 10, 0); // 토
        LocalDateTime day2 = LocalDateTime.of(2025, 11, 2, 9, 0);  // 일
        
        List<WhenToMeet> inputSlots = List.of(
            // Day 1 (토)
            new WhenToMeet(day1, day1.plusMinutes(15), 2L), // 10:00
            new WhenToMeet(day1.plusMinutes(15), day1.plusMinutes(30), 2L), // 10:15
            new WhenToMeet(day1.plusMinutes(30), day1.plusMinutes(45), 1L), // 10:30
            // Day 2 (일)
            new WhenToMeet(day2, day2.plusMinutes(15), 3L), // 09:00
            new WhenToMeet(day2.plusMinutes(15), day2.plusMinutes(30), 3L), // 09:15
            new WhenToMeet(day2.plusMinutes(30), day2.plusMinutes(45), 2L)  // 09:30
        );
        
        // [윈도우 분석 (2슬롯)]
        // Day 1:
        // - 10:00~10:30 [2, 2] -> min: 2
        // - 10:15~10:45 [2, 1] -> min: 1
        // Day 2:
        // - 09:00~09:30 [3, 3] -> min: 3
        // - 09:15~09:45 [3, 2] -> min: 2
        
        // WHEN
        List<WhenToMeetRecommendResponseDto> result = whenToMeetLogicService.recommendBestSlots(
            inputSlots, durationMinutes, topN, memberCnt
        );
        
        // THEN
        assertThat(result).hasSize(3);
        
        // 1순위: Day 2 (최적)
        assertThat(result.get(0).available()).isEqualTo(3L);
        assertThat(result.get(0).status()).isEqualTo("최적");
        assertThat(result.get(0).week()).isEqualTo("일");
        assertThat(result.get(0).startTime()).isEqualTo(day2); // 09:00
        
        // 2순위: Day 1 (보통) - 11월 1일 10:00
        assertThat(result.get(1).available()).isEqualTo(2L);
        assertThat(result.get(1).status()).isEqualTo("보통");
        assertThat(result.get(1).week()).isEqualTo("토");
        assertThat(result.get(1).startTime()).isEqualTo(day1); // 10:00
        
        // 3순위: Day 2 (보통) - 11월 2일 09:15
        assertThat(result.get(2).available()).isEqualTo(2L);
        assertThat(result.get(2).status()).isEqualTo("보통");
        assertThat(result.get(2).week()).isEqualTo("일");
        assertThat(result.get(2).startTime()).isEqualTo(day2.plusMinutes(15)); // 09:15
    }
    
    @Test
    @DisplayName("recommendBestSlotsV2: [15분 슬롯] 60분 요청 시, 정렬(참여율, 시간) 및 Top3가 올바른지 검증")
    void recommendBestSlotsV2_With15MinSlots_Success_Top3() {
        // GIVEN
        Long slotTime = 15L;          // 15분 단위 슬롯
        Long durationMinutes = 60L; // 60분 (requiredSlots = 4)
        Long topN = 3L;
        Long memberCnt = 2L;
        
        LocalDateTime day = LocalDateTime.of(2025, 10, 30, 9, 0); // 목요일
        
        // (시나리오 GIVEN은 V1 테스트와 동일)
        // 09:00~10:00 (4슬롯) : [2, 2, 2, 2] -> min: 2
        // 10:00~11:00 (4슬롯) : [1, 1, 1, 1] -> min: 1
        // 11:00~12:00 (4슬롯) : [2, 2, 2, 2] -> min: 2
        // 09:15~10:15 (4슬롯) : [2, 2, 2, 1] -> min: 1
        
        List<WhenToMeet> inputSlots = List.of(
            // 09:00 ~ 10:00 (2명)
            new WhenToMeet(day, day.plusMinutes(15), 2L),
            new WhenToMeet(day.plusMinutes(15), day.plusMinutes(30), 2L),
            new WhenToMeet(day.plusMinutes(30), day.plusMinutes(45), 2L),
            new WhenToMeet(day.plusMinutes(45), day.plusMinutes(60), 2L),
            // 10:00 ~ 11:00 (1명)
            new WhenToMeet(day.plusMinutes(60), day.plusMinutes(75), 1L),
            new WhenToMeet(day.plusMinutes(75), day.plusMinutes(90), 1L),
            new WhenToMeet(day.plusMinutes(90), day.plusMinutes(105), 1L),
            new WhenToMeet(day.plusMinutes(105), day.plusMinutes(120), 1L),
            // 11:00 ~ 12:00 (2명)
            new WhenToMeet(day.plusMinutes(120), day.plusMinutes(135), 2L),
            new WhenToMeet(day.plusMinutes(135), day.plusMinutes(150), 2L),
            new WhenToMeet(day.plusMinutes(150), day.plusMinutes(165), 2L),
            new WhenToMeet(day.plusMinutes(165), day.plusMinutes(180), 2L)
        );
        
        // [정렬 순서 예상]
        // 1. 09:00~10:00 (available=2, startTime=09:00)
        // 2. 11:00~12:00 (available=2, startTime=11:00)
        // 3. 09:15~10:15 (available=1, startTime=09:15)
        
        // WHEN
        List<WhenToMeetRecommendResponseDto> result = whenToMeetLogicService.recommendBestSlotsV2(
            inputSlots, slotTime, durationMinutes, topN, memberCnt
        );
        
        // THEN
        assertThat(result).hasSize(3);
        
        // 1순위
        assertThat(result.get(0).available()).isEqualTo(2L);
        assertThat(result.get(0).status()).isEqualTo("최적");
        assertThat(result.get(0).startTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 9, 0));
        assertThat(result.get(0).endTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 10, 0));
        
        // 2순위
        assertThat(result.get(1).available()).isEqualTo(2L);
        assertThat(result.get(1).status()).isEqualTo("최적");
        assertThat(result.get(1).startTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 11, 0));
        assertThat(result.get(1).endTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 12, 0));
        
        // 3순위
        assertThat(result.get(2).available()).isEqualTo(1L);
        assertThat(result.get(2).status()).isEqualTo("보통");
        assertThat(result.get(2).startTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 9, 15));
        assertThat(result.get(2).endTime()).isEqualTo(LocalDateTime.of(2025, 10, 30, 10, 15));
    }
    
    @Test
    @DisplayName("recommendBestSlotsV2: [30분 슬롯] 90분 요청 시, requiredSlots(3)가 올바르게 계산되는지 검증")
    void recommendBestSlotsV2_With30MinSlots_Success() {
        // GIVEN
        Long slotTime = 30L;          // 30분 단위 슬롯
        Long durationMinutes = 90L; // 90분
        Long topN = 2L;
        Long memberCnt = 5L;
        
        // V2 로직: (90 + 30 - 1) / 30 = 119 / 30 = 3
        // 즉, 3개의 연속된 슬롯(30분짜리 3개 = 90분)이 필요합니다.
        
        LocalDateTime day = LocalDateTime.of(2025, 11, 10, 10, 0); // 월
        
        // GIVEN: 30분 단위 슬롯 리스트
        List<WhenToMeet> inputSlots = List.of(
            new WhenToMeet(day, day.plusMinutes(30), 5L),                 // 10:00-10:30 (Slot 0)
            new WhenToMeet(day.plusMinutes(30), day.plusMinutes(60), 5L), // 10:30-11:00 (Slot 1)
            new WhenToMeet(day.plusMinutes(60), day.plusMinutes(90), 5L), // 11:00-11:30 (Slot 2)
            new WhenToMeet(day.plusMinutes(90), day.plusMinutes(120), 3L), // 11:30-12:00 (Slot 3) - "Bad Slot"
            new WhenToMeet(day.plusMinutes(120), day.plusMinutes(150), 5L),// 12:00-12:30 (Slot 4)
            new WhenToMeet(day.plusMinutes(150), day.plusMinutes(180), 5L) // 12:30-13:00 (Slot 5)
        );
        
        // [윈도우 분석 (3슬롯)]
        // (A) 10:00~11:30 (Slots 0,1,2): [5, 5, 5] -> min: 5
        // (B) 10:30~12:00 (Slots 1,2,3): [5, 5, 3] -> min: 3
        // (C) 11:00~12:30 (Slots 2,3,4): [5, 3, 5] -> min: 3
        // (D) 11:30~13:00 (Slots 3,4,5): [3, 5, 5] -> min: 3
        
        // [정렬 순서 예상 (Top 2)]
        // 1. (A) 10:00~11:30 (available=5)
        // 2. (B) 10:30~12:00 (available=3, startTime=10:30)
        
        // WHEN
        List<WhenToMeetRecommendResponseDto> result = whenToMeetLogicService.recommendBestSlotsV2(
            inputSlots, slotTime, durationMinutes, topN, memberCnt
        );
        
        // THEN
        assertThat(result).hasSize(2);
        
        // 1순위
        assertThat(result.get(0).available()).isEqualTo(5L);
        assertThat(result.get(0).status()).isEqualTo("최적");
        assertThat(result.get(0).week()).isEqualTo("월");
        assertThat(result.get(0).startTime()).isEqualTo(LocalDateTime.of(2025, 11, 10, 10, 0));
        assertThat(result.get(0).endTime()).isEqualTo(LocalDateTime.of(2025, 11, 10, 11, 30));
        
        // 2순위
        assertThat(result.get(1).available()).isEqualTo(3L);
        assertThat(result.get(1).status()).isEqualTo("보통");
        assertThat(result.get(1).week()).isEqualTo("월");
        assertThat(result.get(1).startTime()).isEqualTo(LocalDateTime.of(2025, 11, 10, 10, 30));
        assertThat(result.get(1).endTime()).isEqualTo(LocalDateTime.of(2025, 11, 10, 12, 0));
    }
}