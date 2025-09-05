package unischedule.member.service;

import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.entity.Member;
import unischedule.member.repository.MemberRepository;

public interface MemberService {
    void registerMember(MemberRegistrationDto registrationDto);
    boolean isMemberExists(String email);

}
