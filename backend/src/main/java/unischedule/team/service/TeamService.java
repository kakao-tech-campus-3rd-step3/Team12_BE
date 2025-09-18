package unischedule.team.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unischedule.team.dto.TeamCreateRequestDto;
import unischedule.team.dto.TeamCreateResponseDto;
import unischedule.team.entity.Team;
import unischedule.team.repository.TeamRepository;

@Service
@RequiredArgsConstructor
public class TeamService {
    
    private final TeamRepository teamRepository;
    
    @Transactional
    public TeamCreateResponseDto createTeam(TeamCreateRequestDto requestDto) {
        
        TeamCodeGenerator teamCodeGenerator = new TeamCodeGenerator();
        
        String code;
        do {
            code = teamCodeGenerator.generate();
        } while (teamRepository.existsByInviteCode(code));
        
        Team newTeam = new Team(requestDto.name(), code);
        
        Team saved = teamRepository.save(newTeam);
        
        return new TeamCreateResponseDto(
            saved.getTeamId(),
            saved.getName(),
            saved.getInviteCode()
        );
    }
}
