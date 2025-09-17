package unischedule.event.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.events.entity.Event;
import unischedule.events.repository.EventRepository;

@DataJpaTest
@EntityScan(basePackages = "unischedule")
class EventRepositoryTest {
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private CalendarRepository calendarRepository;
    
    @Test
    @DisplayName("특정 캘린더 이벤트 저장 후 조회")
    void saveAndFind() {
        // given
        Calendar calendar = new Calendar(null, null, "test", "test-calendar");

        Calendar savedCalendar = calendarRepository.save(calendar);

        Event event = new Event("회의", "주간 회의",
            LocalDateTime.now(), LocalDateTime.now(),
            "CONFIRMED", true
        );
        event.connectCalendar(savedCalendar);
        
        // when
        eventRepository.save(event);
        List<Event> events = eventRepository.findScheduleInPeriod(
                List.of(savedCalendar.getCalendarId()),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                LocalDateTime.of(2025, 9, 30, 23, 59)
        );
        
        // then
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getTitle()).isEqualTo("회의");
        assertThat(events.get(0).getEventId()).isNotNull(); // DB가 자동 생성한 id 확인
    }
}
