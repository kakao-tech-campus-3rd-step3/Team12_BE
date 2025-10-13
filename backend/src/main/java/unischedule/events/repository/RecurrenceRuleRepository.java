package unischedule.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.events.domain.RecurrenceRule;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {
}
