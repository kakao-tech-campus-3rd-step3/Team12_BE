package unischedule.mocks;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import unischedule.member.dto.MemberRegistrationDto;
import unischedule.member.repository.MemberRepository;
import unischedule.member.service.MemberService;
import unischedule.team.dto.TeamCreateRequestDto;
import unischedule.team.dto.TeamJoinRequestDto;
import unischedule.team.service.TeamService;

/**
 * 애플리케이션 시작 시 테스트용 사용자를 DB에 삽입하는 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class MockDataInitializer implements CommandLineRunner {
    private final MemberService memberService;
    private final TeamService teamService;
    private final MemberRepository memberRepository;

    public void run(String... args) {
    }

    public void createMockUsers() {
        MemberRegistrationDto member1 = new MemberRegistrationDto("testuser1@email.com", "test-user1", "12345678");
        if (memberRepository.findByEmail(member1.email()).isEmpty()) {
            memberService.registerMember(member1);
        }

        MemberRegistrationDto member2 = new MemberRegistrationDto("testuser2@email.com", "test-user2", "12345678");
        if (memberRepository.findByEmail(member2.email()).isEmpty()) {
            memberService.registerMember(member2);
        }
    }

    public void createTeams() {
        String email1 = "testuser1@email.com";
        TeamCreateRequestDto requestDto = new TeamCreateRequestDto("Test Team 1");
        TeamCreateRequestDto requestDto2 = new TeamCreateRequestDto("Test Team 2");
        teamService.createTeam(email1, requestDto);
        teamService.createTeam(email1, requestDto2);
    }

    public void joinTeams() {
        String email2 = "testuser2@email.com";
        TeamJoinRequestDto requestDto = new TeamJoinRequestDto("f3QmZ&");
        teamService.joinTeam(email2, requestDto);
    }
}
