package unischedule.team.service.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
        // --- Given (준비) ---
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
        
        // --- When (실행) ---
        whenToMeetLogicService.applyMemberEvents(slots, members, intervalStarts, intervalEnds, whenToMeetRawService);
        
        // --- Then (검증) ---
        // Alice의 일정(9:00~9:30)과 겹치는 슬롯
        assertThat(slots.get(0).getAvailableMember()).isEqualTo(1L); // 9:00~9:15
        assertThat(slots.get(1).getAvailableMember()).isEqualTo(1L); // 9:15~9:30
        
        // 겹치지 않는 슬롯
        assertThat(slots.get(2).getAvailableMember()).isEqualTo(2L); // 9:30~9:45 (기존 2명 그대로)
    }
}