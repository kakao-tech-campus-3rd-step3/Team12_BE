package unischedule.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import unischedule.calendar.repository.CalendarRepository;
import unischedule.exception.dto.EntityAlreadyExistsException;
import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.domain.Member;
import unischedule.member.repository.MemberRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

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
}
