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
            
            for (EventGetResponseDto event : events) {
                applyOverlap(slots, event, start, end);
            }
        }
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
    
    private void applyOverlap(List<WhenToMeet> slots,
        EventGetResponseDto event,
        LocalDateTime intervalStart,
        LocalDateTime intervalEnd) {
        
        LocalDateTime eventStart = event.startTime();
        LocalDateTime eventEnd = event.endTime();
        
        for (WhenToMeet slot : slots) {
            if (slot.getStartTime().isBefore(intervalStart) || slot.getEndTime().isAfter(intervalEnd))
                continue;
            
            if (slot.getStartTime().isBefore(eventEnd) && slot.getEndTime().isAfter(eventStart)) {
                slot.discountAvailable();
            }
        }
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
