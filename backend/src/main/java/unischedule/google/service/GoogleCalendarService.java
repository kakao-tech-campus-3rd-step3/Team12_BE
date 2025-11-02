package unischedule.google.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.domain.Event;
import unischedule.events.dto.EventCreateDto;
import unischedule.events.service.common.EventCommandService;
import unischedule.events.service.internal.EventRawService;
import unischedule.google.domain.GoogleAuthToken;
import unischedule.google.repository.GoogleAuthTokenRepository;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {
    private final MemberRawService memberRawService;
    private final EventCommandService eventCommandService;
    private final EventRawService eventRawService;
    private final CalendarRawService calendarRawService;
    private final GoogleAuthTokenRepository tokenRepository;

    public void syncEvents(String userEmail) {
        Member member = memberRawService.findMemberByEmail(userEmail);
        GoogleAuthToken token = tokenRepository.findByMember(member)
                .orElseThrow(() -> new IllegalArgumentException("Google 계정이 연동되지 않았습니다."));

        //GoogleCredential credential = new GoogleCredential().Builder()
        //        .setClientSecretes()


    }

    private void mapAndSaveEvents(List<com.google.api.services.calendar.model.Event> googleEvents, Member member) {
        Calendar personalCalendar = calendarRawService.getMyPersonalCalendar(member);

        for (com.google.api.services.calendar.model.Event googleEvent : googleEvents) {
            if ("cancelled".equals(googleEvent.getStatus())) {
                continue;
            }

            EventCreateDto dto = mapGoogleEventToDto(googleEvent);

            Event newEvent = Event.builder()
                    .title(dto.title())
                    .content(dto.description())
                    .startAt(dto.startTime())
                    .endAt(dto.endTime())
                    .build();

            newEvent.connectCalendar(personalCalendar);
            eventRawService.saveEvent(newEvent);
        }
    }

    private EventCreateDto mapGoogleEventToDto(com.google.api.services.calendar.model.Event googleEvent) {
        LocalDateTime start = convertGoogleDateTime(googleEvent.getStart());
        LocalDateTime end = convertGoogleDateTime(googleEvent.getEnd());

        // 종일 일정
        if (googleEvent.getStart().getDate() != null) {
            start = LocalDate.parse(googleEvent.getStart().getDate().toStringRfc3339()).atStartOfDay();
            end = LocalDate.parse(googleEvent.getEnd().getDate().toStringRfc3339()).atStartOfDay();
        }

        return new EventCreateDto(
                googleEvent.getSummary(),
                googleEvent.getDescription(),
                start,
                end
        );
    }

    private LocalDateTime convertGoogleDateTime(EventDateTime eventDateTime) {
        if (eventDateTime == null) return null;
        DateTime dateTime = eventDateTime.getDateTime();
        if (dateTime == null) {
            // 종일 일정일 경우
            return null;
        }

        return Instant.ofEpochMilli(dateTime.getValue())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
