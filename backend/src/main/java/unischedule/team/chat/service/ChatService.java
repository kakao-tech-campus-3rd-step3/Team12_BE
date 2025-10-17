package unischedule.team.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.team.chat.dto.ChatMessageDto;
import unischedule.team.chat.dto.ChatMessageHistoryResponseDto;
import unischedule.team.chat.dto.ChatMessageRequestDto;
import unischedule.team.chat.entity.ChatMessage;
import unischedule.team.chat.repository.ChatMessageRepository;
import unischedule.member.domain.Member;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.domain.Team;
import unischedule.team.service.internal.TeamRawService;
import unischedule.team.service.internal.TeamMemberRawService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberRawService memberRawService;
    private final TeamRawService teamRawService;
    private final TeamMemberRawService teamMemberRawService;

    public ChatMessageHistoryResponseDto getMessages(Long teamId, String userEmail, Long cursor, int size) {
        Team team = teamRawService.findTeamById(teamId);
        Member member = memberRawService.findMemberByEmail(userEmail);
        teamMemberRawService.validateMembership(team, member);
        
        Pageable pageable = PageRequest.of(0, size);

        Slice<ChatMessage> slice;
        if (cursor != null) {
            slice = chatMessageRepository.findByTeamAndIdLessThanOrderByIdDesc(team, cursor, pageable);
        } else {
            slice = chatMessageRepository.findByTeamOrderByIdDesc(team, pageable);
        }

        List<ChatMessage> messages = slice.getContent();
        List<ChatMessageDto> messageDtos = messages.stream()
                .map(ChatMessageDto::from)
                .collect(Collectors.toList());

        Long nextCursor = (slice.hasNext() && !messages.isEmpty())
                ? messages.getLast().getId()
                : null;

        return new ChatMessageHistoryResponseDto(messageDtos, slice.hasNext(), nextCursor);
    }

    @Transactional
    public ChatMessageDto sendMessage(Long teamId, String senderEmail, ChatMessageRequestDto requestDto) {
        Team team = teamRawService.findTeamById(teamId);
        Member sender = memberRawService.findMemberByEmail(senderEmail);
        teamMemberRawService.validateMembership(team, sender);

        ChatMessage message = ChatMessage.builder()
                .team(team)
                .sender(sender)
                .senderName(sender.getNickname())
                .content(requestDto.content())
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        return ChatMessageDto.from(savedMessage);
    }
}
