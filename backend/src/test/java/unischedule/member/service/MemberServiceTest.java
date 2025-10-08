package unischedule.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.exception.EntityNotFoundException;
import unischedule.exception.dto.EntityAlreadyExistsException;
import unischedule.member.dto.CurrentMemberInfoResponseDto;
import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.domain.Member;
import unischedule.member.repository.MemberRepository;
import unischedule.member.service.internal.MemberRawService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRawService memberRawService;

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private CalendarRepository calendarRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입")
    void registerMember() {
        // given
        MemberRegistrationDto dto = new MemberRegistrationDto("test@example.com", "nickname", "1q2w3e4r!");
        given(memberRepository.findByEmail(dto.email())).willReturn(Optional.empty());
        given(passwordEncoder.encode(dto.password())).willReturn("encodedPassword");

        // when
        memberService.registerMember(dto);

        // then
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void registerMemberDuplicated() {
        // given
        MemberRegistrationDto dto = new MemberRegistrationDto("test@example.com", "nickname", "1q2w3e4r!");
        given(memberRepository.findByEmail(dto.email())).willReturn(Optional.of(new Member(dto.email(), dto.nickname(), dto.password())));

        // when & then
        assertThrows(EntityAlreadyExistsException.class, () -> {
            memberService.registerMember(dto);
        });
    }

    @Test
    @DisplayName("회원 정보를 정상적으로 조회한다")
    void 회원정보_정상조회() {
        // given
        String email = "testuser@email.com";
        Member member = new Member(
                "testuser@email.com",
                "test-user",
                "123456789"
        );
        given(memberRawService.findMemberByEmail(member.getEmail())).willReturn(member);
        // when
        CurrentMemberInfoResponseDto responseDto = memberService.getCurrentMemberInfo(email);

        // then
        assertNotNull(responseDto);
        assertEquals(email, responseDto.email());
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 예외를 던진다")
    void 존재하지않는회원_예외발생() {
        // given
        String email = "notfound@email.com";
        given(memberRawService.findMemberByEmail(email)).willThrow(new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // when & then
        assertThrows(EntityNotFoundException.class, () -> {
            memberService.getCurrentMemberInfo(email);
        });
    }
}
