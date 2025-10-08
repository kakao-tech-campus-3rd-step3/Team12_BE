package unischedule.team.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.chat.dto.ChatMessageDto;
import unischedule.team.chat.dto.ChatMessageHistoryResponseDto;
import unischedule.team.chat.dto.ChatMessageRequestDto;
import unischedule.team.chat.entity.ChatMessage;
import unischedule.team.chat.repository.ChatMessageRepository;
import unischedule.team.domain.Team;
import unischedule.team.service.internal.TeamMemberRawService;
import unischedule.team.service.internal.TeamRawService;
import unischedule.util.TestUtil;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private MemberRawService memberRawService;
    @Mock
    private TeamRawService teamRawService;
    @Mock
    private TeamMemberRawService teamMemberRawService;
    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        Team team = TestUtil.makeTeam();
        Member member = TestUtil.makeMember();

        ReflectionTestUtils.setField(team, "teamId", 1L);
        ReflectionTestUtils.setField(member, "memberId", 10L);
        ReflectionTestUtils.setField(member, "nickname", "테스터");

        when(teamRawService.findTeamById(1L)).thenReturn(team);
        when(memberRawService.findMemberByEmail("user@test.com")).thenReturn(member);
        doNothing().when(teamMemberRawService).validateMembership(team, member);
    }

    @Test
    @DisplayName("커서 없이 조회 - 최신 메시지 50개 반환")
    void getMessagesWithoutCursor() {
        List<ChatMessage> messages = List.of(createMessage(3L), createMessage(2L));
        Slice<ChatMessage> slice = new SliceImpl<>(messages, PageRequest.of(0, 50), true);

        when(chatMessageRepository.findByTeamIdOrderByIdDesc(1L, PageRequest.of(0, 50)))
                .thenReturn(slice);

        ChatMessageHistoryResponseDto result = chatService.getMessages(1L, "user@test.com", null, 50);

        assertThat(result.messages()).hasSize(2);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(2L);
    }

    @Test
    @DisplayName("커서로 조회 - 이전 메시지 반환")
    void getMessagesWithCursor() {
        List<ChatMessage> messages = List.of(createMessage(1L));
        Slice<ChatMessage> slice = new SliceImpl<>(messages, PageRequest.of(0, 50), false);

        when(chatMessageRepository.findByTeamIdAndIdLessThanOrderByIdDesc(1L, 3L, PageRequest.of(0, 50)))
                .thenReturn(slice);

        ChatMessageHistoryResponseDto result = chatService.getMessages(1L, "user@test.com", 3L, 50);

        assertThat(result.messages()).hasSize(1);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    @DisplayName("메시지 전송 - DB 저장 후 DTO 반환")
    void sendMessage() {
        ChatMessage savedMessage = createMessage(100L);
        
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageDto result = chatService.sendMessage(1L, "user@test.com", 
                new ChatMessageRequestDto("안녕하세요"));

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.content()).isEqualTo("안녕하세요");
        assertThat(result.senderName()).isEqualTo("테스터");
    }

    private ChatMessage createMessage(Long id) {
        ChatMessage message = ChatMessage.builder()
                .teamId(1L)
                .senderId(10L)
                .senderName("테스터")
                .content("안녕하세요")
                .build();
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.now());
        return message;
    }
}
