package unischedule.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.entity.Member;
import unischedule.member.repository.MemberRepository;

@Service
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberServiceImpl(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void registerMember(MemberRegistrationDto registrationDto) {
        memberRepository.save(new Member(
                registrationDto.email(),
                registrationDto.nickname(),
                passwordEncoder.encode(registrationDto.password())
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMemberExists(String email) {
        return memberRepository.findByEmail(email).isPresent();
    }
}
