package unischedule.util;

import unischedule.calendar.entity.Calendar;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventState;
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
                EventState.CONFIRMED,
                false
        );
    }

    public static Calendar makePersonalCalendar(Member owner) {
        return new Calendar(
                owner,
                "My Calendar",
                "Personal calendar"
        );
    }

    public static Calendar makeTeamCalendar(Member owner, Team team) {
        return new Calendar(
                owner,
                "Team Calendar",
                "Team calendar"
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

