package unischedule.events.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import unischedule.events.domain.Event;
import unischedule.events.repository.EventExceptionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventFinder {

    private final EventRawService eventRawService;
    private final EventExceptionRepository eventExceptionRepository;

    public List<Event> expandEvents(List<Event> allCandidateEvents){
        return List.of();
    }
}
