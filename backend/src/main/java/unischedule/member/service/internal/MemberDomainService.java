package unischedule.member.service.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.exception.EntityNotFoundException;
import unischedule.member.entity.Member;
import unischedule.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
public class MemberDomainService {
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
    }
}
