package unischedule.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.auth.repository.RefreshTokenRepository;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.events.domain.Event;
import unischedule.events.service.common.EventCommandService;
import unischedule.events.service.internal.EventParticipantRawService;
import unischedule.events.service.internal.EventRawService;
import unischedule.exception.InvalidInputException;
import unischedule.exception.dto.EntityAlreadyExistsException;
import unischedule.member.domain.Member;
import unischedule.member.dto.CurrentMemberInfoResponseDto;
import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.repository.MemberRepository;
import unischedule.member.service.internal.MemberRawService;
import unischedule.team.chat.repository.ChatMessageRepository;
import unischedule.team.domain.TeamMember;
import unischedule.team.domain.TeamRole;
import unischedule.team.service.internal.TeamMemberRawService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final CalendarRepository calendarRepository;
    private final MemberRawService memberRawService;

    private final RefreshTokenRepository refreshTokenRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TeamMemberRawService teamMemberRawService;
    private final EventRawService eventRawService;
    private final EventParticipantRawService eventParticipantRawService;
    private final EventCommandService eventCommandService;

    /**
     * 회원가입 시 기본 개인 캘린더 생성
     *
     * @param registrationDto
     */
    @Transactional
    public void registerMember(MemberRegistrationDto registrationDto) {
        if (memberRepository.findByEmail(registrationDto.email()).isPresent()) {
            throw new EntityAlreadyExistsException("이미 사용중인 이메일입니다");
        }

        Member newMember = new Member(
                registrationDto.email(),
                registrationDto.nickname(),
                passwordEncoder.encode(registrationDto.password())
        );

        memberRepository.save(newMember);

        Calendar personalCalendar = new Calendar(
                newMember
        );
        calendarRepository.save(personalCalendar);
    }

    /**
     * 현재 로그인한 회원 정보 조회
     *
     * @param email 현재 로그인한 회원 이메일
     * @return CurrentMemberInfoResponseDto 회원 정보
     */
    @Transactional(readOnly = true)
    public CurrentMemberInfoResponseDto getCurrentMemberInfo(String email) {
        Member member = memberRawService.findMemberByEmail(email);

        return CurrentMemberInfoResponseDto.from(member);
    }

    @Transactional
    public void withdrawMember(String email) {
        Member member = memberRawService.findMemberByEmail(email);

        checkTeamWithdraw(member);

        deletePersonalCalendarData(member);

        teamMemberRawService.deleteAllByMember(member);
        eventParticipantRawService.deleteAllByMember(member);

        refreshTokenRepository.findByMember(member)
                .ifPresent(refreshTokenRepository::delete);

        chatMessageRepository.updateSenderNameBySender(member, "탈퇴한 사용자");

        member.withdraw();
        memberRepository.save(member);

    }

    private void checkTeamWithdraw(Member member) {
        List<TeamMember> memberships = teamMemberRawService.findByMember(member);

        for (TeamMember tm : memberships) {
            checkTeamLeadership(tm);
        }
    }

    private void checkTeamLeadership(TeamMember teamMember) {
        if (teamMember.getRole() != TeamRole.LEADER) {
            return;
        }

        int leaderCount = teamMemberRawService.countByTeamAndRole(teamMember.getTeam(), TeamRole.LEADER);
        if (leaderCount <= 1) {
            throw new InvalidInputException("팀[" + teamMember.getTeam().getName() + "] 탈퇴 전 팀장을 위임하거나 팀을 삭제해야 합니다.");
        }
    }

    /**
     * 개인 캘린더 데이터 삭제
     * @param member
     */
    private void deletePersonalCalendarData(Member member) {
        calendarRepository.findByOwnerAndTeamIsNull(member)
                .ifPresent(personalCalendar -> {
                    List<Event> events = eventRawService.findByCalendar(personalCalendar);

                    for (Event event : events) {
                        if (event.getRecurrenceRule() == null) {
                            eventCommandService.deleteSingleEvent(event);
                        }
                        else {
                            eventCommandService.deleteRecurringEvent(event);
                        }
                    }

                    calendarRepository.delete(personalCalendar);
                });
    }
}
