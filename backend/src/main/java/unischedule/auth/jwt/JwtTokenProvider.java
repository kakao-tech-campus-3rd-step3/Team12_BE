package unischedule.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.stream.Collectors;

public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long accessTokenTimeoutMs;
    private final long refreshTokenTimeoutMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}")
            String secretKey,
            @Value("{jwt.accessTokenTimeoutSec}")
            long accessTokenTimeoutSec,
            @Value("${jwt.refreshTokenTimeoutSec}")
            long refreshTokenTimeoutSec
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTimeoutMs = accessTokenTimeoutSec * 1000;
        this.refreshTokenTimeoutMs = refreshTokenTimeoutSec * 1000;
    }

    public String createAccessToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenTimeoutMs);

        return Jwts.builder()
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }
}
