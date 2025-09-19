package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.events.dto.EventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.events.entity.EventState;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.entity.Event;
import unischedule.events.repository.EventRepository;
import unischedule.member.entity.Member;
import unischedule.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EventService {
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final CalendarRepository calendarRepository;

    @Transactional
    public EventCreateResponseDto makePersonalEvent(String email, EventCreateRequestDto requestDto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Calendar targetCalendar = calendarRepository.findById(requestDto.calendarId())
                .orElseThrow(() -> new EntityNotFoundException("해당 캘린더를 찾을 수 없습니다."));

        targetCalendar.validateOwner(member);

        boolean conflict = eventRepository.existsPersonalScheduleInPeriod(
                member.getMemberId(),
                requestDto.startTime(),
                requestDto.endTime()
        );

        if (conflict) {
            throw new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다.");
        }

        Event newEvent = Event.builder()
                .title(requestDto.title())
                .content(requestDto.description())
                .startAt(requestDto.startTime())
                .endAt(requestDto.endTime())
                .state(EventState.CONFIRMED)
                .isPrivate(requestDto.isPrivate())
                .build();

        newEvent.connectCalendar(targetCalendar);
        Event saved = eventRepository.save(newEvent);

        return EventCreateResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getPersonalEvents(String email, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<Event> findEvents = eventRepository.findPersonalScheduleInPeriod(
                member.getMemberId(),
                startAt,
                endAt
        );

        return findEvents.stream()
                .map(EventGetResponseDto::from)
                .toList();
    }
    
    @Transactional
    public EventGetResponseDto modifyPersonalEvent(String email, EventModifyRequestDto requestDto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Event findEvent = eventRepository.findById(requestDto.eventId())
            .orElseThrow(() -> new EntityNotFoundException("해당 이벤트가 없습니다."));

        findEvent.validateEventOwner(member);

        if (requestDto.startTime() != null || requestDto.endTime() != null) {
            LocalDateTime newStartAt = requestDto.startTime() != null ? requestDto.startTime() : findEvent.getStartAt();
            LocalDateTime newEndAt = requestDto.endTime() != null ? requestDto.endTime() : findEvent.getEndAt();

            boolean conflict = eventRepository.existsPersonalScheduleInPeriodExcludingEvent(
                    member.getMemberId(),
                    newStartAt,
                    newEndAt,
                    requestDto.eventId()
            );
            
            if (conflict) {
                throw new InvalidInputException("해당 시간에 겹치는 일정이 있어 수정할 수 없습니다.");
            }
        }
        
        findEvent.modifyEvent(requestDto);
        
        return EventGetResponseDto.from(findEvent);
    }

    @Transactional
    public void deletePersonalEvent(String email, Long eventId) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("해당 일정을 찾을 수 없습니다."));

        event.validateEventOwner(member);

        eventRepository.delete(event);
    }
}
