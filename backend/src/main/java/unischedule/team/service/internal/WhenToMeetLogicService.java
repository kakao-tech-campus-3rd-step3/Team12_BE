package unischedule.team.service.internal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
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
    
    public void applyMemberEvents(List<WhenToMeet> slots,
        List<Member> members,
        List<LocalDateTime> intervalStarts,
        List<LocalDateTime> intervalEnds,
        WhenToMeetRawService rawService) {
        
        for (Member member : members) {
            for (int i = 0; i < intervalStarts.size(); i++) {
                LocalDateTime start = intervalStarts.get(i);
                LocalDateTime end = intervalEnds.get(i);
                List<EventGetResponseDto> events = rawService.findMemberEvents(member, start, end);
                
                for (EventGetResponseDto event : events) {
                    applyOverlap(slots, event, start, end);
                }
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
