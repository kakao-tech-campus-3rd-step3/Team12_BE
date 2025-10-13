package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.RRule;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.dto.EventUpdateDto;
import unischedule.events.repository.EventRepository;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.member.domain.Member;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EventRawService {
    private final EventRepository eventRepository;

    @Transactional
    public Event saveEvent(Event event) {
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Event findEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("해당 일정을 찾을 수 없습니다."));
    }

    @Transactional
    public void deleteEvent(Event event) {
        eventRepository.delete(event);
    }

    @Transactional
    public void updateEvent(Event event, EventUpdateDto updateDto) {
        event.modifyEvent(
                updateDto.title(),
                updateDto.content(),
                updateDto.startTime(),
                updateDto.endTime(),
                updateDto.isPrivate()
        );
    }

    @Transactional(readOnly = true)
    public void validateNoSchedule(Member member, LocalDateTime startTime, LocalDateTime endTime) {
        if (eventRepository.existsPersonalScheduleInPeriod(member.getMemberId(), startTime, endTime)) {
            throw new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public void validateNoScheduleForRecurrence(Member member, LocalDateTime firstStartTime, LocalDateTime firstEndTime, String rruleString) {
        try {
            RRule<ZonedDateTime> rrule = new RRule<>(rruleString);
            Recur<ZonedDateTime> recur = rrule.getRecur();

            ZonedDateTime seed = firstStartTime.atZone(ZoneId.systemDefault());
            ZonedDateTime endBoundary = getValidationEndDate(firstStartTime, rruleString)
                    .atZone(ZoneId.systemDefault());

            List<ZonedDateTime> eventStartTimeListZdt = recur.getDates(seed, endBoundary);
            Duration duration = Duration.between(firstStartTime, firstEndTime);

            for (ZonedDateTime startZdt : eventStartTimeListZdt) {
                LocalDateTime eventStartTime = startZdt.toLocalDateTime();
                LocalDateTime eventEndTime = startZdt.plus(duration).toLocalDateTime();
                validateNoSchedule(member, eventStartTime, eventEndTime);
            }
        }
        catch (RuntimeException e) {
            throw new InvalidInputException("유효하지 않은 반복 규칙(RRULE) 형식입니다.");
        }
    }

    @Transactional(readOnly = true)
    public void validateNoScheduleForMembers(List<Member> memberList, LocalDateTime startTime, LocalDateTime endTime) {
        if (memberList.isEmpty()) return;
        List<Long> memberIds = memberList
                .stream()
                .map(Member::getMemberId)
                .toList();

        if (eventRepository.existsScheduleForMembers(memberIds, startTime, endTime)) {
            throw new InvalidInputException("일정이 겹치는 멤버가 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public void canUpdateEventForMembers(List<Member> memberList, Event event, LocalDateTime startTime, LocalDateTime endTime) {
        if (memberList.isEmpty()) return;
        List<Long> memberIds = memberList
                .stream()
                .map(Member::getMemberId)
                .toList();

        if (eventRepository.existsScheduleForMembersExcludingEvent(
                memberIds,
                startTime,
                endTime,
                event.getEventId())
        ) {
            throw new InvalidInputException("일정이 겹치는 멤버가 있어서 일정을 수정할 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<Event> findSchedule(List<Long> calendarIds, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findEventsInCalendarsInPeriod(calendarIds, startTime, endTime);
    }

    @Transactional(readOnly = true)
    public List<Event> findSingleSchedule(List<Long> calendarIds, LocalDateTime startTime, LocalDateTime endTime) {
        return eventRepository.findSingleEventsInPeriod(calendarIds, startTime, endTime);
    }

    @Transactional(readOnly = true)
    public List<Event> findRecurringSchedule(List<Long> calendarIds, LocalDateTime endTime) {
        return eventRepository.findRecurringEventsInPeriod(calendarIds, endTime);
    }

    @Transactional(readOnly = true)
    public void canUpdateEvent(Member member, Event event, LocalDateTime startTime, LocalDateTime endTime) {
        if (eventRepository.existsPersonalScheduleInPeriodExcludingEvent(member.getMemberId(), startTime, endTime, event.getEventId())) {
            throw new InvalidInputException("해당 시간에 겹치는 일정이 있어 수정할 수 없습니다.");
        }
    }
    
    @Transactional(readOnly = true)
    public List<Event> findUpcomingEventsByMember(Member member) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 3); // 개수 제한
        return eventRepository.findUpcomingEvents(member.getMemberId(), now, pageable);
    }
    
    @Transactional(readOnly = true)
    public List<Event> findTodayEventsByMember(Member member) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();              // 오늘 00:00
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();    // 내일 00:00
        return eventRepository.findPersonalScheduleInPeriod(member.getMemberId(), startOfDay, endOfDay);
    }

    private LocalDateTime getValidationEndDate(LocalDateTime startTime, String rruleString) {
        // 시스템 부하 방지를 위해 반복 일정은 최대 2년으로 설정
        LocalDateTime maxEndDate = startTime.plusYears(2);

        Pattern pattern = Pattern.compile("UNTIL=([0-9]{8}T[0-9]{6}Z)");
        Matcher matcher = pattern.matcher(rruleString.toUpperCase());

        if (matcher.find()) {
            String until = matcher.group(1);
            int year = Integer.parseInt(until.substring(0, 4));
            int month = Integer.parseInt(until.substring(4, 6));
            int day = Integer.parseInt(until.substring(6, 8));

            LocalDateTime untilEndDate = LocalDateTime.of(year, month, day, 23, 59, 59);

            if (untilEndDate.isBefore(maxEndDate)) {
                return untilEndDate;
            }
            else {
                return maxEndDate;
            }
        }
        // 기한이 없는 경우 최대 2년으로 설정
        return maxEndDate;
    }
}
