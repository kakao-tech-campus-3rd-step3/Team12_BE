package unischedule.team.service.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.member.domain.Member;
import unischedule.team.domain.WhenToMeet;
import unischedule.team.dto.WhenToMeetResponseDto;

@Service
public class WhenToMeetLogicService {
    
    private record DailyInterval(LocalDateTime start, LocalDateTime end) {}
    
    public List<LocalDateTime> generateIntervalStarts() {
        LocalDate today = LocalDate.now();
        LocalDateTime effectiveStart = today.plusDays(1).atTime(9, 0);
        LocalDateTime effectiveEnd = today.plusDays(8).atStartOfDay();
        
        return generateIntervalStarts(effectiveStart, effectiveEnd);
    }
    
    public List<LocalDateTime> generateIntervalEnds() {
        LocalDate today = LocalDate.now();
        LocalDateTime effectiveStart = today.plusDays(1).atTime(9, 0);
        LocalDateTime effectiveEnd = today.plusDays(8).atStartOfDay();
        
        return generateIntervalEnds(effectiveStart, effectiveEnd);
    }
    
    public List<LocalDateTime> generateIntervalStarts(LocalDateTime start, LocalDateTime end) {
        return generateDailyIntervals(start, end)
            .stream()
            .map(DailyInterval::start)
            .toList();
    }
    
    public List<LocalDateTime> generateIntervalEnds(LocalDateTime start, LocalDateTime end) {
        return generateDailyIntervals(start, end)
            .stream()
            .map(DailyInterval::end)
            .toList();
    }
    
    public List<WhenToMeet> generateSlots(List<Member> members,
        List<LocalDateTime> starts,
        List<LocalDateTime> ends) {
        List<WhenToMeet> slots = new ArrayList<>();
        
        for (int i = 0; i < starts.size(); i++) {
            LocalDateTime start = starts.get(i);
            LocalDateTime end = ends.get(i);
            LocalDateTime cursor = start;
            
            while (cursor.isBefore(end)) {
                LocalDateTime slotStart = cursor;
                LocalDateTime slotEnd = cursor.plusMinutes(15);
                if (slotEnd.isAfter(end)) slotEnd = end;
                
                slots.add(new WhenToMeet(slotStart, slotEnd, (long) members.size()));
                cursor = slotEnd;
            }
        }
        return slots;
    }
    
    @Transactional
    public void applyMemberEvents(List<WhenToMeet> slots,
        List<Member> members,
        List<LocalDateTime> intervalStarts,
        List<LocalDateTime> intervalEnds,
        WhenToMeetRawService rawService) {
        
        LocalDateTime start = intervalStarts.get(0);
        LocalDateTime end = intervalEnds.get(intervalEnds.size() - 1);
        
        for (Member member : members) {
            List<EventGetResponseDto> events = rawService.findMemberEvents(member, start, end);
            
            if (events.isEmpty()) {
                continue;
            }
            
            for (WhenToMeet slot : slots) {
                // "이 멤버가 이 슬롯에 바쁜가?"를 확인
                boolean isBusyInThisSlot = isMemberBusyForSlot(slot, events);
                
                if (isBusyInThisSlot) {
                    slot.discountAvailable();
                }
            }
        }
    }
    
    /**
     * 주어진 슬롯(WhenToMeet)이 멤버의 일정(events) 목록 중 하나라도 겹치는지 확인합니다.
     *
     * @param slot   확인할 시간 슬롯
     * @param events 멤버의 전체 일정 목록
     * @return 겹치는 일정이 하나라도 있으면 true, 그렇지 않으면 false
     */
    private boolean isMemberBusyForSlot(WhenToMeet slot, List<EventGetResponseDto> events) {
        for (EventGetResponseDto event : events) {
            // (SlotStart < EventEnd) AND (SlotEnd > EventStart)
            boolean isOverlap = slot.getStartTime().isBefore(event.endTime()) &&
                slot.getEndTime().isAfter(event.startTime());
            
            if (isOverlap) {
                // 하나라도 겹치면 이 멤버는 이 슬롯에서 '바쁨'
                return true;
            }
        }
        // 모든 일정을 확인했는데도 겹치지 않으면 '가능'
        return false;
    }
    
    public List<WhenToMeetResponseDto> toResponse(List<WhenToMeet> slots) {
        return slots.stream()
            .map(slot -> new WhenToMeetResponseDto(
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getAvailableMember()
            ))
            .toList();
    }
    
    private List<DailyInterval> generateDailyIntervals(LocalDateTime start, LocalDateTime end) {
        List<DailyInterval> intervals = new ArrayList<>();
        LocalDate current = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        
        while (!current.isAfter(endDate)) {
            LocalDateTime dayStart = current.atTime(9, 0);
            LocalDateTime dayEnd = current.plusDays(1).atTime(0, 0);
            
            // 시작일이면 start와 09:00 중 늦은 시간
            if (current.equals(start.toLocalDate())) {
                dayStart = start.isAfter(dayStart) ? start : dayStart;
            }
            
            // 종료일이면 24:00와 end 중 빠른 시간
            if (current.equals(end.toLocalDate())) {
                dayEnd = end.isBefore(dayEnd) ? end : dayEnd;
            }
            
            // 시작 < 종료인 경우만 (유효한 교집합이 있는 경우)
            if (!dayStart.isAfter(dayEnd)) {
                intervals.add(new DailyInterval(dayStart, dayEnd));
            }
            
            current = current.plusDays(1);
        }
        return intervals;
    }
}
