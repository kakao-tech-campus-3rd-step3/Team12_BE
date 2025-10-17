package unischedule.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.calendar.entity.Calendar;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.exception.dto.EntityAlreadyExistsException;
import unischedule.member.domain.Member;
import unischedule.member.dto.CurrentMemberInfoResponseDto;
import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.repository.MemberRepository;
import unischedule.member.service.internal.MemberRawService;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final CalendarRepository calendarRepository;
    private final MemberRawService memberRawService;

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
}
