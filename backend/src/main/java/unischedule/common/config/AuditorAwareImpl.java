package unischedule.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import unischedule.member.entity.Member;
import unischedule.member.repository.MemberRepository;

import java.util.Optional;

@RequiredArgsConstructor
public class AuditorAwareImpl implements AuditorAware<Member> {
    private final MemberRepository memberRepository;

    @Override
    public Optional<Member> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        String email = authentication.getName();
        return memberRepository.findByEmail(email);
    }
}
