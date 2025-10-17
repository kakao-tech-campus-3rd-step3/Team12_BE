package unischedule.team.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.team.chat.entity.ChatMessage;
import unischedule.team.domain.Team;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Slice<ChatMessage> findByTeamOrderByIdDesc(Team team, Pageable pageable);

    Slice<ChatMessage> findByTeamAndIdLessThanOrderByIdDesc(Team team, Long messageId, Pageable pageable);

}
