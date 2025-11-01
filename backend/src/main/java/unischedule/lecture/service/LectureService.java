package unischedule.lecture.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.domain.RecurrenceRule;
import unischedule.events.service.internal.EventRawService;
import unischedule.events.service.internal.RecurrenceRuleRawService;
import unischedule.lecture.domain.Lecture;
import unischedule.lecture.dto.LectureCreateResponseDto;
import unischedule.lecture.dto.LectureResponseDto;
import unischedule.lecture.dto.LecturesCreateRequestDto;
import unischedule.lecture.dto.LecturesCreateResponseDto;
import unischedule.lecture.everytime.dto.TimetableDetailDto;
import unischedule.lecture.service.internal.LectureRawService;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureService {
    
    private final LectureRawService lectureRawService;
    private final MemberRawService memberRawService;
    private final CalendarRawService calendarRawService;
    private final EventRawService eventRawService;
    private final RecurrenceRuleRawService recurrenceRuleRawService;
    
    public List<LectureResponseDto> getMyLectures(String email) {
        Member member = memberRawService.findMemberByEmail(email);
        return lectureRawService.findActiveLecturesByMemberId(member.getMemberId()).stream()
                .map(LectureResponseDto::from)
                .toList();
    }
    
    @Transactional
    public LecturesCreateResponseDto saveLectures(String email, LecturesCreateRequestDto requestDto) {
        Member member = memberRawService.findMemberByEmail(email);
        
        List<Lecture> activeLectures = lectureRawService.findActiveLecturesByMemberId(member.getMemberId());
        activeLectures.forEach(lecture -> {
            lectureRawService.deleteLecture(lecture);
            eventRawService.deleteEvent(lecture.getEvent());
        });
        
        Calendar calendar = calendarRawService.getMyPersonalCalendar(member);
        
        List<LectureCreateResponseDto> responses = requestDto.timetable().subjects().stream()
                .map(subject -> createLecture(calendar, subject, requestDto.startDate(), requestDto.endDate()))
                .toList();
        
        return new LecturesCreateResponseDto(responses);
    }
    
    private LectureCreateResponseDto createLecture(
            Calendar calendar, 
            TimetableDetailDto.Subject subject,
            LocalDate startDate,
            LocalDate endDate
    ) {
        TimetableDetailDto.Subject.Time firstTime = subject.times().getFirst();
        
        RecurrenceRule recurrenceRule = recurrenceRuleRawService.saveRecurrenceRule(
                new RecurrenceRule(createRruleString(subject.times(), endDate))
        );
        
        String content = firstTime.place() != null 
                ? String.format("%s | %s", subject.professor(), firstTime.place())
                : subject.professor();
        
        Event event = Event.builder()
                .title(subject.name())
                .content(content)
                .startAt(calculateEventDateTime(startDate, firstTime.dayOfWeek(), firstTime.startTime()))
                .endAt(calculateEventDateTime(startDate, firstTime.dayOfWeek(), firstTime.endTime()))
                .isPrivate(false)
                .build();
        
        event.connectCalendar(calendar);
        event.connectRecurrenceRule(recurrenceRule);
        Event savedEvent = eventRawService.saveEvent(event);
        
        Lecture lecture = Lecture.builder()
                .name(subject.name())
                .professor(subject.professor())
                .credit(subject.credit())
                .startDate(startDate)
                .endDate(endDate)
                .build();
        
        lecture.connectEvent(savedEvent);
        Lecture savedLecture = lectureRawService.saveLecture(lecture);
        
        return new LectureCreateResponseDto(
                savedLecture.getLectureId(),
                savedEvent.getEventId(),
                subject.name(),
                subject.professor(),
                subject.credit(),
                subject.times()
        );
    }
    
    private LocalDateTime calculateEventDateTime(LocalDate startDate, Integer dayOfWeek, LocalTime time) {
        DayOfWeek targetDay = DayOfWeek.of(dayOfWeek);
        LocalDate eventDate = startDate.with(TemporalAdjusters.nextOrSame(targetDay));
        return LocalDateTime.of(eventDate, time);
    }
    
    private String createRruleString(List<TimetableDetailDto.Subject.Time> times, LocalDate endDate) {
        String byDay = times.stream()
                .map(time -> convertDayOfWeekToRrule(time.dayOfWeek()))
                .distinct()
                .sorted()
                .collect(Collectors.joining(","));
        
        String until = endDate.atTime(23, 59, 59)
                .format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        return String.format("FREQ=WEEKLY;BYDAY=%s;UNTIL=%s", byDay, until);
    }
    
    private String convertDayOfWeekToRrule(Integer dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "MO";
            case 2 -> "TU";
            case 3 -> "WE";
            case 4 -> "TH";
            case 5 -> "FR";
            case 6 -> "SA";
            case 7 -> "SU";
            default -> throw new IllegalArgumentException("Invalid dayOfWeek");
        };
    }
}

