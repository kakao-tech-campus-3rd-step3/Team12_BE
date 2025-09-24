package unischedule.util;

import unischedule.calendar.entity.Calendar;
import unischedule.events.entity.Event;
import unischedule.events.entity.EventState;
import unischedule.member.entity.Member;

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

    public static Calendar makeCalendar(Member owner) {
        return new Calendar(
                owner,
                "My Calendar",
                "Personal calendar"
        );
    }
}
