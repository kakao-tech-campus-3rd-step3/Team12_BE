//package unischedule.mocks;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//import unischedule.member.dto.MemberRegistrationDto;
//import unischedule.member.repository.MemberRepository;
//import unischedule.member.service.MemberService;
//
///**
// * 애플리케이션 시작 시 테스트용 사용자를 DB에 삽입하는 컴포넌트입니다.
// */
//@Component
//@RequiredArgsConstructor
//public class UserMock implements CommandLineRunner {
//    private final MemberService memberService;
//    private final MemberRepository memberRepository;
//
//    public void run(String... args) {
//        MemberRegistrationDto member1 = new MemberRegistrationDto("test1@email.com", "user1", "12345678");
//        if (memberRepository.findByEmail(member1.email()).isEmpty()) {
//            memberService.registerMember(member1);
//        }
//
//        MemberRegistrationDto member2 = new MemberRegistrationDto("test2@email.com", "user2", "12345678");
//        if (memberRepository.findByEmail(member2.email()).isEmpty()) {
//            memberService.registerMember(member2);
//        }
//    }
//}
