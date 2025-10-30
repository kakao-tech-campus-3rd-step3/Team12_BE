package unischedule.events.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecurrenceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurrence_rule_id")
    private Long id;

    @Column(nullable = false)
    private String rruleString;

    public RecurrenceRule(String rruleString) {
        this.rruleString = rruleString;
    }

    public void updateRruleString(String rruleString) {
        if (rruleString != null && !rruleString.isBlank()) {
            this.rruleString = rruleString;
        }
    }
}
