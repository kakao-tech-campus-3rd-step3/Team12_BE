package unischedule.team.chat.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import unischedule.member.domain.Member;
import unischedule.team.chat.entity.ChatMessage;
import unischedule.team.domain.Team;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Slice<ChatMessage> findByTeamOrderByIdDesc(Team team, Pageable pageable);

    Slice<ChatMessage> findByTeamAndIdLessThanOrderByIdDesc(Team team, Long messageId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("""
            UPDATE ChatMessage cm
            SET cm.senderName = :newName
            WHERE cm.sender = :sender
    """)
    void updateSenderNameBySender(
            @Param("sender") Member sender,
            @Param("newName") String newName
    );
}
