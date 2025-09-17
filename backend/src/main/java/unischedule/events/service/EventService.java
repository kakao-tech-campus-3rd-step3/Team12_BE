package unischedule.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.events.dto.EventCreateRequestDto;
import unischedule.events.dto.EventCreateResponseDto;
import unischedule.events.dto.EventModifyRequestDto;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.InvalidInputException;
import unischedule.events.dto.EventGetResponseDto;
import unischedule.events.entity.Event;
import unischedule.events.repository.EventRepository;
import unischedule.member.entity.Member;
import unischedule.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final CalendarRepository calendarRepository;

    @Transactional
    public EventCreateResponseDto makeEvent(String email, EventCreateRequestDto requestDto) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<Calendar> calendars = calendarRepository.findByOwner(member);

        List<Long> calendarIds = calendars.stream()
                .map(Calendar::getCalendarId)
                .toList();

        boolean conflict = eventRepository.existsScheduleInPeriod(calendarIds, requestDto.endTime(), requestDto.startTime());

        if (conflict) {
            throw new InvalidInputException("겹치는 일정이 있어 등록할 수 없습니다.");
        }

        Event newEvent = Event.builder()
                .title(requestDto.title())
                .content(requestDto.description())
                .startAt(requestDto.startTime())
                .endAt(requestDto.endTime())
                .state("CONFIRMED")
                .isPrivate(requestDto.isPrivate())
                .build();

        Event saved = eventRepository.save(newEvent);

        return new EventCreateResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<EventGetResponseDto> getEvents(String email, LocalDateTime startAt, LocalDateTime endAt) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        List<Calendar> calendars = calendarRepository.findByOwner(member);

        if (calendars.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> calendarIds = calendars.stream()
                .map(Calendar::getCalendarId)
                .toList();

        List<Event> findEvents = eventRepository.findScheduleInPeriod(calendarIds, endAt, startAt);

        return findEvents.stream()
                .map(EventGetResponseDto::new)
                .toList();
    }

    //수정은 현재 테크 스펙 상 다른 도메인에 있어, 추후 추가 작성 필요
    //삭제는 현재 테크 스펙 상 없음
    
    @Transactional
    public EventGetResponseDto modifyEvent(String email, Long eventId, EventModifyRequestDto requestDto) {
        Event findEvent = eventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("해당 이벤트가 없습니다."));
        
        if (requestDto.startTime() != null || requestDto.endTime() != null) {

            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

            List<Calendar> calendars = calendarRepository.findByOwner(member);

            List<Long> calendarIds = calendars.stream()
                    .map(Calendar::getCalendarId)
                    .toList();

            boolean conflict = eventRepository.existsScheduleInPeriod(
                    calendarIds,
                    requestDto.endTime() != null ? requestDto.endTime() : findEvent.getEndAt(),
                    requestDto.startTime() != null ? requestDto.startTime() : findEvent.getStartAt()
            );
            
            if (conflict) {
                throw new InvalidInputException("해당 시간에 겹치는 일정이 있어 수정할 수 없습니다.");
            }
        }
        
        findEvent.modifyEvent(requestDto);
        
        return new EventGetResponseDto(findEvent);
    }
}
