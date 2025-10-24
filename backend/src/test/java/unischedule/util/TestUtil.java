package unischedule.util;

import unischedule.calendar.entity.Calendar;
import unischedule.events.domain.Event;
import unischedule.events.domain.RecurrenceRule;
import unischedule.member.domain.Member;
import unischedule.team.domain.Team;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;

import java.time.LocalDateTime;

public class TestUtil {

    public static Member makeMember() {
        return new Member(
                "test@example.com",
                "nickname",
                "1q2w3e4r!"
        );
    }

    public static Event makeEvent(String title, String content) {
        return new Event(
                title,
                content,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                false
        );
    }

    public static Event makeRecurringEvent(String title, String content) {
        Event event = new Event(
                title,
                content,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                false
        );

        // 매주 반복
        RecurrenceRule recurrenceRule = new RecurrenceRule("FREQ=WEEKLY;INTERVAL=1");
        event.connectRecurrenceRule(recurrenceRule);
        return event;
    }

    public static Calendar makePersonalCalendar(Member owner) {
        return new Calendar(
                owner
        );
    }

    public static Calendar makeTeamCalendar(Member owner, Team team) {
        return new Calendar(
                owner,
                team
        );
    }

    public static Team makeTeam() {
        return new Team(
                "test team",
                "description",
                "ABCDEF"
        );
    }

    public static TeamMember makeTeamMember(Team team, Member member) {
        return new TeamMember(team, member, TeamRole.MEMBER);
    }
}

