package unischedule.team.service.internal;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.member.domain.Member;
import unischedule.team.domain.WhenToMeet;
import unischedule.team.dto.WhenToMeetResponseDto;

@Service
public class WhenToMeetLogicService {
    public List<LocalDateTime> generateIntervalStarts() {
        return IntStream.rangeClosed(1, 7)
            .mapToObj(i -> LocalDate.now().plusDays(i).atTime(9, 0))
            .toList();
    }
    
    public List<LocalDateTime> generateIntervalEnds() {
        return IntStream.rangeClosed(1, 7)
            .mapToObj(i -> LocalDate.now().plusDays(i + 1).atStartOfDay())
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
    
    @Transactional(readOnly = true)
    public void applyMemberEvents(List<WhenToMeet> slots,
        List<Member> members,
        List<LocalDateTime> intervalStarts,
        List<LocalDateTime> intervalEnds,
        WhenToMeetRawService rawService) {
        
        final long SLOT_MINUTES = 15L;
        int slotsPerDay = (int) Duration.between(LocalDateTime.of(0,1,1,9,0),
                LocalDateTime.of(0,1,2,0,0))
            .toMinutes() / (int)SLOT_MINUTES; // 09~24시 기준
        
        for (Member member : members) {
            // 멤버의 일주일 일정 조회 (내일부터 7일)
            LocalDateTime weekStart = intervalStarts.get(0);
            LocalDateTime weekEnd = intervalEnds.get(intervalEnds.size() - 1);
            List<EventGetResponseDto> events = rawService.findMemberEvents(member, weekStart, weekEnd);
            
            for (EventGetResponseDto event : events) {
                LocalDateTime eventStart = event.startTime();
                LocalDateTime eventEnd = event.endTime();
                
                // 이벤트가 일주일 범위 밖이면 무시
                if (eventEnd.isBefore(weekStart) || eventStart.isAfter(weekEnd)) continue;
                
                // 이벤트가 걸친 날짜별로 처리
                LocalDate startDate = eventStart.toLocalDate();
                LocalDate endDate = eventEnd.toLocalDate();
                
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    // 하루 슬롯 범위
                    LocalDateTime dayStart = date.atTime(9, 0);
                    LocalDateTime dayEnd = date.plusDays(1).atTime(0, 0);
                    
                    // 하루 슬롯 범위 안에서 이벤트 잘라서 계산
                    LocalDateTime effectiveStart = eventStart.isBefore(dayStart) ? dayStart : eventStart;
                    LocalDateTime effectiveEnd = eventEnd.isAfter(dayEnd) ? dayEnd : eventEnd;
                    
                    // 슬롯 인덱스 계산 (해당 날짜 시작 기준)
                    int dayOffset = (int) Duration.between(intervalStarts.get(0).toLocalDate().atTime(9,0), date.atTime(9,0)).toDays() * slotsPerDay;
                    int startIdx = dayOffset + (int) Duration.between(dayStart, effectiveStart).toMinutes() / (int)SLOT_MINUTES;
                    int endIdx = dayOffset + (int) Duration.between(dayStart, effectiveEnd).toMinutes() / (int)SLOT_MINUTES;
                    
                    // 해당 범위 슬롯만 차감
                    for (int i = startIdx; i <= endIdx && i < slots.size(); i++) {
                        WhenToMeet slot = slots.get(i);
                        if (slot.getStartTime().isBefore(effectiveEnd) && slot.getEndTime().isAfter(effectiveStart)) {
                            slot.discountAvailable();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 이벤트 시간에 대응하는 슬롯 인덱스를 계산 (15분 단위)
     */
    private int calculateSlotIndex(LocalDateTime firstSlotStart,
        LocalDateTime time,
        long slotMinutes,
        int totalSlots) {
        
        long diffMinutes = java.time.Duration.between(firstSlotStart, time).toMinutes();
        int index = (int) (diffMinutes / slotMinutes);
        
        if (index < 0) return 0;
        if (index >= totalSlots) return totalSlots - 1;
        return index;
    }
    /*
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
    */
    public List<WhenToMeetResponseDto> toResponse(List<WhenToMeet> slots) {
        return slots.stream()
            .map(slot -> new WhenToMeetResponseDto(
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getAvailableMember()
            ))
            .toList();
    }
}
