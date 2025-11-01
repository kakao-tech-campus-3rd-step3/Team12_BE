package unischedule.events.service.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventOverride;
import unischedule.events.domain.RecurrenceRule;
import unischedule.events.dto.EventCreateDto;
import unischedule.events.dto.EventUpdateDto;
import unischedule.events.dto.RecurringEventCreateRequestDto;
import unischedule.events.dto.RecurringInstanceDeleteRequestDto;
import unischedule.events.dto.RecurringInstanceModifyRequestDto;
import unischedule.events.service.internal.EventOverrideRawService;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.service.internal.RecurrenceRuleRawService;
import unischedule.events.service.internal.RecurringEventRawService;
import unischedule.exception.InvalidInputException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventCommandService {
    private final EventRawService eventRawService;
    private final EventOverrideRawService eventOverrideRawService;
    private final RecurringEventRawService recurringEventRawService;
    private final RecurrenceRuleRawService recurrenceRuleRawService;

    @Transactional
    public Event createSingleEvent(
            Calendar targetCalendar,
            EventCreateDto createDto
    ) {
           Event newEvent = Event.builder()
                   .title(createDto.title())
                   .content(createDto.description())
                   .startAt(createDto.startTime())
                   .endAt(createDto.endTime())
                   .isPrivate(createDto.isPrivate())
                   .isSelective(false)
                   .build();

           newEvent.connectCalendar(targetCalendar);
           return eventRawService.saveEvent(newEvent);
    }

    @Transactional
    public Event createRecurringEvent(Calendar targetCalendar, RecurringEventCreateRequestDto requestDto) {

        Event newEvent = Event.builder()
                .title(requestDto.title())
                .content(requestDto.description())
                .startAt(requestDto.firstStartTime())
                .endAt(requestDto.firstEndTime())
                .isPrivate(requestDto.isPrivate())
                .isSelective(false)
                .build();

        RecurrenceRule rrule = new RecurrenceRule(requestDto.rrule());
        recurrenceRuleRawService.saveRecurrenceRule(rrule);

        newEvent.connectRecurrenceRule(rrule);
        newEvent.connectCalendar(targetCalendar);

        return eventRawService.saveEvent(newEvent);
    }

    @Transactional
    public Event modifySingleEvent(Event eventToModify, EventUpdateDto updateDto) {
        if (eventToModify.getRecurrenceRule() != null) {
            throw new InvalidInputException("단일 일정이 아닙니다.");
        }

        modifyEvent(eventToModify, updateDto);
        return eventToModify;
    }

    @Transactional
    public EventOverride modifyRecurringInstance(Event originalEvent, RecurringInstanceModifyRequestDto requestDto) {
        Optional<EventOverride> eventOverrideOpt = eventOverrideRawService.findEventOverride(originalEvent, requestDto.originalStartTime());

        if (eventOverrideOpt.isPresent()) {
            EventOverride presentEventOverride = eventOverrideOpt.get();
            eventOverrideRawService.updateEventOverride(presentEventOverride, requestDto.toEventOverrideUpdateDto());
            return eventOverrideRawService.saveEventOverride(presentEventOverride);
        }

        EventOverride eventOverride = EventOverride.makeEventOverride(originalEvent, requestDto.toEventOverrideDto());
        return eventOverrideRawService.saveEventOverride(eventOverride);
    }

    @Transactional
    public Event modifyRecurringEvent(
            Event eventToModify,
            EventUpdateDto updateDto
    ) {
        if (eventToModify.getRecurrenceRule() == null) {
            throw new InvalidInputException("반복 일정이 아닙니다.");
        }

        modifyEvent(eventToModify, updateDto);
        eventOverrideRawService.deleteAllEventOverrideByEvent(eventToModify);
        return eventToModify;
    }

    private void modifyEvent(
            Event eventToModify,
            EventUpdateDto updateDto
    ) {
        eventRawService.updateEvent(eventToModify, updateDto);
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


    /**
     * 반복 일정 단건 삭제 로직
     * 기존 override가 있다면 markAsDeleted
     * 기존 override가 없다면 eventDeleteOverride 생성
     * @param originalEvent
     * @param requestDto
     */
    @Transactional
    public void deleteRecurringEventInstance(Event originalEvent, RecurringInstanceDeleteRequestDto requestDto) {

        Optional<EventOverride> eventOverrideOpt = eventOverrideRawService.findEventOverride(originalEvent, requestDto.originalStartTime());

        if (eventOverrideOpt.isPresent()) {
            EventOverride targetOverride = eventOverrideOpt.get();
            if (targetOverride.isDeleteOverride()) {
                return;
            }
            targetOverride.markAsDeleted();
            return;
        }

        if (eventOverrideRawService.existsDeleteEventOverride(originalEvent, requestDto.originalStartTime())) {
            return;
        }

        EventOverride eventOverride = EventOverride.makeEventDeleteOverride(originalEvent, requestDto.originalStartTime());
        eventOverrideRawService.saveEventOverride(eventOverride);
    }
}
