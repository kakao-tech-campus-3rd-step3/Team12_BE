package unischedule.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventParticipant;
import unischedule.member.domain.Member;

import java.util.List;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {

    List<EventParticipant> findByEvent(Event event);

    boolean existsByEventAndMember(Event event, Member member);

    void deleteAllByEvent(Event event);

    void deleteAllByMember(Member member);
}
