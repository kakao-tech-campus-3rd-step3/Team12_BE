package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import unischedule.events.domain.RecurrenceRule;
import unischedule.events.repository.RecurrenceRuleRepository;

@Service
@RequiredArgsConstructor
public class RecurrenceRuleRawService {
    private final RecurrenceRuleRepository recurrenceRuleRepository;

    public RecurrenceRule saveRecurrenceRule(RecurrenceRule rrule) {
        return recurrenceRuleRepository.save(rrule);
    }
}
