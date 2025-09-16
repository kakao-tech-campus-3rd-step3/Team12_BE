package unischedule.users.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import unischedule.events.entity.Event;
import unischedule.events.repository.EventRepository;

@DataJpaTest
class EventRepositoryTest {
    @Autowired
    private EventRepository eventRepository;
    
    @Test
    void testSaveAndFind() {
        // given
        Event event = new Event(1L, "회의", "주간 회의",
            LocalDateTime.of(2025, 9, 10, 10, 0),
            LocalDateTime.of(2025, 9, 10, 11, 0),
            "CONFIRMED", true
        );
        
        // when
        Event saved = eventRepository.save(event);
        List<Event> events = eventRepository.findByCreatorIdAndStartAtLessThanAndEndAtGreaterThan(
            1L,
            LocalDateTime.of(2025, 9, 30, 23, 59),
            LocalDateTime.of(2025, 9, 1, 0, 0)
            
        );
        
        // then
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getTitle()).isEqualTo("회의");
        assertThat(saved.getId()).isNotNull(); // DB가 자동 생성한 id 확인
    }
}
