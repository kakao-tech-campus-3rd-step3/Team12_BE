package unischedule.member.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'ACTIVE'")
    private MemberStatus status;

    protected Member() {

    }

    public Member(String email, String nickname, String password) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.status = MemberStatus.ACTIVE;
    }

    public boolean isEqualMember(Member other) {
        return Objects.equals(this.memberId, other.memberId);
    }

    /**
     * 회원 탈퇴
     */
    public void withdraw() {
        this.email = "deleted_member_" + this.memberId + "@unischedule.com";
        this.nickname = "탈퇴한 사용자";
        this.password = "DELETED_USER_INVALID_PASSWORD_HASH";
        this.status = MemberStatus.DELETED;
    }
}
