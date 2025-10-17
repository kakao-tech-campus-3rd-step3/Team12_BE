package unischedule.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.Objects;

@Entity
@Getter
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;

    @Column(nullable = false, length = 50)
    private String nickname;
    @Column(nullable = false, updatable = false, length = 50, unique = true)
    private String email;
    @Column(nullable = false, length = 255)
    private String password;

    protected Member() {

    }

    public Member(String email, String nickname, String password) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
    }

    public boolean isEqualMember(Member other) {
        return Objects.equals(this.memberId, other.memberId);
    }
}
