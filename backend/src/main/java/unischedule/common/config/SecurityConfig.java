package unischedule.common.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.SerializationUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import unischedule.auth.jwt.JwtAuthenticationFilter;
import unischedule.auth.jwt.JwtTokenProvider;
import unischedule.google.handler.OAuth2LoginSuccessHandler;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // 인증되지 않은 경우 401 반환
    @Bean
    public AuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:3000", "https://unischedule.vercel.app"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
                return customizeAuthorizationRequest(request, authorizationRequest);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
                return customizeAuthorizationRequest(request, authorizationRequest);
            }

            private OAuth2AuthorizationRequest customizeAuthorizationRequest(HttpServletRequest request, OAuth2AuthorizationRequest authorizationRequest) {
                if (authorizationRequest == null) {
                    return null;
                }

                String customState = request.getParameter("state");
                OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.from(authorizationRequest);

                if (customState != null && !customState.isBlank()) {
                    builder.state(customState);
                }

                return builder.build();
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/members/login", "/api/members/signup", "/api/members/refresh", "/ws/**", "/api/auth/email/send", "/api/auth/email/verify").permitAll()
                        .requestMatchers("/error", "/actuator/health", "/test").permitAll()
                        // OAuth
                        .requestMatchers("/login/oauth2/**", "/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(customAuthorizationRequestResolver)
                                .authorizationRequestRepository(new StatelessAuthorizationRequestRepository()))
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint())
                );
        return http.build();
    }

    public static class StatelessAuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
        public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
        private static final int COOKIE_EXPIRE_SECONDS = 180;

        @Override
        public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
            // 요청에서 쿠키를 읽어서 복원
            return getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                    .map(this::deserialize)
                    .orElse(null);
        }

        @Override
        public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
            if (authorizationRequest == null) {
                deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
                return;
            }
            addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);
        }

        @Override
        public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
            OAuth2AuthorizationRequest authorizationRequest = this.loadAuthorizationRequest(request);
            if (authorizationRequest != null) {
                deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            }
            return authorizationRequest;
        }

        private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if (cookie.getName().equals(name)) {
                        return Optional.of(cookie);
                    }
                }
            }
            return Optional.empty();
        }

        private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
            ResponseCookie cookie = ResponseCookie.from(name, value)
                    .path("/")
                    .httpOnly(true)
                    .secure(true)
                    .maxAge(maxAge)
                    .sameSite("None")
                    .build();

            response.addHeader("Set-Cookie", cookie.toString());
        }

        private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
            getCookie(request, name).ifPresent(cookie -> {
                ResponseCookie deletedCookie = ResponseCookie.from(name, "")
                        .path("/")
                        .httpOnly(true)
                        .secure(true)
                        .maxAge(0)
                        .sameSite("None")
                        .build();

                response.addHeader("Set-Cookie", deletedCookie.toString());
            });
        }
        private String serialize(Object object) {
            return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
        }

        private OAuth2AuthorizationRequest deserialize(jakarta.servlet.http.Cookie cookie) {
            byte[] data = Base64.getUrlDecoder().decode(cookie.getValue());
            try {
                return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(data);
            } catch (Exception e) {
                return null;
            }
        }
    }
}

