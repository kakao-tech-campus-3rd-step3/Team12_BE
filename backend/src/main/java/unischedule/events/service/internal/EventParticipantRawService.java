package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.events.domain.Event;
import unischedule.events.domain.EventParticipant;
import unischedule.events.repository.EventParticipantRepository;
import unischedule.member.domain.Member;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventParticipantRawService {
    private final EventParticipantRepository eventParticipantRepository;

    @Transactional(readOnly = true)
    public List<Member> getParticipantsForEvent(Event event) {
        return eventParticipantRepository.findByEvent(event)
                .stream()
                .map(EventParticipant::getMember)
                .toList();
    }

    @Transactional
    public void saveAllParticipantsForEvent(Event event, List<Member> participants) {
        List<EventParticipant> newParticipants = participants.stream()
                .map(member -> new EventParticipant(event, member))
                .toList();

        eventParticipantRepository.saveAll(newParticipants);
    }

    @Transactional
    public void deleteAllParticipantsByEvent(Event event) {
        eventParticipantRepository.deleteAllByEvent(event);
    }

    @Transactional
    public void deleteAllByMember(Member member) {
        eventParticipantRepository.deleteAllByMember(member);
    }

    @Transactional
    public void deleteAllByEvent(Event event) {
        eventParticipantRepository.deleteAllByEvent(event);
    }
}
