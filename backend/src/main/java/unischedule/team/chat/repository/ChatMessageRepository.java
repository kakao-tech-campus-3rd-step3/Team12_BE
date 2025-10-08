package unischedule.team.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import unischedule.team.chat.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Slice<ChatMessage> findByTeamIdOrderByIdDesc(Long teamId, Pageable pageable);

    Slice<ChatMessage> findByTeamIdAndIdLessThanOrderByIdDesc(Long teamId, Long messageId, Pageable pageable);

}
