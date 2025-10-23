package unischedule.events.service.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.domain.RecurrenceRule;
import unischedule.events.dto.EventCreateDto;
import unischedule.events.dto.EventModifyDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.service.internal.EventOverrideRawService;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.service.internal.RecurringEventRawService;
import unischedule.exception.InvalidInputException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventCommandService {
    private final EventQueryService eventQueryService;
    private final EventRawService eventRawService;
    private final EventOverrideRawService eventOverrideRawService;
    private final RecurringEventRawService recurringEventRawService;

    @Transactional
    public Event createSingleEvent(
            Calendar targetCalendar,
            List<Long> conflictCheckCalendarIds,
            EventCreateDto createDto
    ) {
           eventQueryService.checkNewSingleEventOverlap(conflictCheckCalendarIds, createDto.startTime(), createDto.endTime());

           Event newEvent = Event.builder()
                   .title(createDto.title())
                   .content(createDto.description())
                   .startAt(createDto.startTime())
                   .endAt(createDto.endTime())
                   .isPrivate(createDto.isPrivate())
                   .build();

           newEvent.connectCalendar(targetCalendar);
           return eventRawService.saveEvent(newEvent);
    }

    @Transactional
    public Event createRecurringEvent(Calendar targetCalendar, List<Long> conflictCheckCalendarIds, RecurringEventCreateRequestDto requestDto) {
        eventQueryService.checkNewRecurringEventOverlap(
                conflictCheckCalendarIds,
                requestDto.firstStartTime(),
                requestDto.firstEndTime(),
                requestDto.rrule()
        );

        Event newEvent = Event.builder()
                .title(requestDto.title())
                .content(requestDto.description())
                .startAt(requestDto.firstStartTime())
                .endAt(requestDto.firstEndTime())
                .isPrivate(requestDto.isPrivate())
                .build();

        RecurrenceRule rrule = new RecurrenceRule(requestDto.rrule());
        newEvent.connectRecurrenceRule(rrule);
        newEvent.connectCalendar(targetCalendar);

        return eventRawService.saveEvent(newEvent);
    }

    @Transactional
    public Event modifySingleEvent(Event eventToModify, List<Long> conflictCheckCalendarIds, EventModifyDto modifyDto) {
        if (eventToModify.getRecurrenceRule() != null) {
            throw new InvalidInputException("단일 일정이 아닙니다.");
        }

        modifyEvent(eventToModify, conflictCheckCalendarIds, modifyDto);
        return eventToModify;
    }

    @Transactional
    public Event modifyRecurringEvent(Event eventToModify, List<Long> conflictCheckCalendarIds, EventModifyDto modifyDto) {
        if (eventToModify.getRecurrenceRule() == null) {
            throw new InvalidInputException("반복 일정이 아닙니다.");
        }

        modifyEvent(eventToModify, conflictCheckCalendarIds, modifyDto);
        eventOverrideRawService.deleteAllEventOverrideByEvent(eventToModify);

        return eventToModify;
    }

    @Transactional
    public EventOverride modifyRecurringInstance(Event originalEvent, RecurringInstanceModifyRequestDto requestDto) {
        Optional<EventOverride> eventOverrideOpt = eventOverrideRawService.findEventOverride(originalEvent, requestDto.originalStartTime());

        EventOverride eventOverride;
        if (eventOverrideOpt.isPresent()) {
            eventOverride = eventOverrideOpt.get();
            eventOverrideRawService.updateEventOverride(eventOverride, requestDto.toEventOverrideDto());
        }
        else {
            eventOverride = EventOverride.makeEventOverride(originalEvent, requestDto.toEventOverrideDto());
        }

        return eventOverrideRawService.saveEventOverride(eventOverride);
    }

    private void modifyEvent(Event eventToModify, List<Long> conflictCheckCalendarIds, EventModifyDto modifyDto) {
        LocalDateTime newStartAt = getValueOrDefault(modifyDto.startTime(), eventToModify.getStartAt());
        LocalDateTime newEndTime = getValueOrDefault(modifyDto.endTime(), eventToModify.getEndAt());

        eventQueryService.checkEventUpdateOverlap(
                conflictCheckCalendarIds,
                newStartAt,
                newEndTime,
                eventToModify
        );

        eventRawService.updateEvent(eventToModify, EventModifyDto.toDto(modifyDto));
    }

    private static <T> T getValueOrDefault(T value, T defaultValue) {
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

    @Transactional
    public void deleteSingleEvent(Event eventToDelete) {
        if (eventToDelete.getRecurrenceRule() != null) {
            throw new InvalidInputException("단일 일정이 아닙니다.");
        }

        eventRawService.deleteEvent(eventToDelete);
    }

    @Transactional
    public void deleteRecurringEvent(Event eventToDelete) {
        if (eventToDelete.getRecurrenceRule() == null) {
            throw new InvalidInputException("반복 일정이 아닙니다.");
        }

        recurringEventRawService.deleteRecurringEvent(eventToDelete);
    }

    @Transactional
    public void deleteRecurringEventInstance(Event originalEvent, RecurringInstanceDeleteRequestDto requestDto) {
        EventOverride eventOverride = EventOverride.makeEventDeleteOverride(originalEvent, requestDto.originalStartTime());
        eventOverrideRawService.saveEventOverride(eventOverride);
    }
}
