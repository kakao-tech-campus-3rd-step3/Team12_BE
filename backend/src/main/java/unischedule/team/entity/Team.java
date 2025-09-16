package unischedule.team.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import unischedule.common.entity.BaseEntity;

@Entity
@Getter
@Table(name = "teams")
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long teamId;
    @Column(nullable = false)
    private String name;
    private String inviteCode;

    protected Team() {

    }
}
