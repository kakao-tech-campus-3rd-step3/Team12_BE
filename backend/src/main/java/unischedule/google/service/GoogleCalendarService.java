package unischedule.google.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.service.internal.CalendarRawService;
import unischedule.events.dto.EventCreateDto;
import unischedule.events.service.common.EventCommandService;
import unischedule.events.service.common.EventQueryService;
import unischedule.exception.InvalidInputException;
import unischedule.google.domain.GoogleAuthToken;
import unischedule.google.repository.GoogleAuthTokenRepository;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.service.internal.TeamMemberRawService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {
    private final MemberRawService memberRawService;
    private final EventCommandService eventCommandService;
    private final CalendarRawService calendarRawService;
    private final GoogleAuthTokenRepository tokenRepository;
    private final TeamMemberRawService teamMemberRawService;
    private final EventQueryService eventQueryService;

    private static final String APPLICATION_NAME = "Unischedule";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String CLIENT_ID;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String CLIENT_SECRET;

    public void syncEvents(String userEmail) {
        Member member = memberRawService.findMemberByEmail(userEmail);
        GoogleAuthToken token = tokenRepository.findByMember(member)
                .orElseThrow(() -> new IllegalStateException("Google 계정이 연동되지 않았습니다."));

        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(CLIENT_ID)
                    .setClientSecret(CLIENT_SECRET)
                    .setRefreshToken(token.getRefreshToken())
                    .build();

            com.google.api.services.calendar.Calendar client =
                    new com.google.api.services.calendar.Calendar.Builder(
                            HTTP_TRANSPORT,
                            JSON_FACTORY,
                            new HttpCredentialsAdapter(credentials)
                    )
                            .setApplicationName(APPLICATION_NAME)
                            .build();

            ZoneId kst = ZoneId.of("Asia/Seoul");
            long startTime = LocalDateTime.now().minusMonths(1).atZone(kst).toInstant().toEpochMilli();
            long endTime = LocalDateTime.now().plusMonths(6).atZone(kst).toInstant().toEpochMilli();

            // 한 달 전 ~ 향후 6개월 일정까지
            Events events = client.events().list("primary")
                    .setTimeMin(new DateTime(startTime)) // 최근 30일
                    .setTimeMax(new DateTime(endTime)) // 앞으로 90일
                    .setMaxResults(1000)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();

            List<com.google.api.services.calendar.model.Event> items = events.getItems();

            if (items != null && !items.isEmpty()) {
                mapAndSaveEvents(items, member);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void mapAndSaveEvents(List<com.google.api.services.calendar.model.Event> googleEvents, Member member) {
        Calendar personalCalendar = calendarRawService.getMyPersonalCalendar(member);

        List<Long> allCalendarIds = getMemberCalendarIds(member);

        for (com.google.api.services.calendar.model.Event googleEvent : googleEvents) {
            if ("cancelled".equals(googleEvent.getStatus())) {
                continue;
            }

            EventCreateDto dto = mapGoogleEventToDto(googleEvent);

            if (dto.startTime() == null || dto.endTime() == null) {
                continue;
            }

            boolean isOverlapping;
            try {
                eventQueryService.checkNewSingleEventOverlapForMember(
                        member,
                        allCalendarIds,
                        dto.startTime(),
                        dto.endTime()
                );
                isOverlapping = false;
            } catch (InvalidInputException e) {
                isOverlapping = true;
            }

            if (!isOverlapping) {
                eventCommandService.createSingleEvent(personalCalendar, dto);
            }
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

    private List<Long> getMemberCalendarIds(Member member) {
        List<Team> teamList = teamMemberRawService.findByMember(member)
                .stream()
                .map(TeamMember::getTeam)
                .toList();

        List<Long> calendarIds = new ArrayList<>();

        // 개인 캘린더
        calendarIds.add(calendarRawService.getMyPersonalCalendar(member).getCalendarId());

        // 팀 캘린더
        List<Long> teamCalendarIds = teamList.stream()
                .map(calendarRawService::getTeamCalendar)
                .map(Calendar::getCalendarId)
                .toList();

        calendarIds.addAll(teamCalendarIds);
        return calendarIds;
    }
}
