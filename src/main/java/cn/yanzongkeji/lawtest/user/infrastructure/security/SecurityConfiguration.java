package cn.yanzongkeji.lawtest.user.infrastructure.security;

import cn.yanzongkeji.lawtest.user.infrastructure.configuration.AuthProperties;
import cn.yanzongkeji.lawtest.user.domain.port.AccessTokenDenylist;
import cn.yanzongkeji.lawtest.user.domain.port.LoginProtection;
import cn.yanzongkeji.lawtest.user.application.exception.AuthStateUnavailableException;
import cn.yanzongkeji.lawtest.user.interfaces.rest.response.AuthErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.Customizer;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Bean
    JwtDecoder jwtDecoder(AuthProperties properties, AccessTokenDenylist denylist) {
        SecretKeySpec key = new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        // 黑名单在 exp 到期时删除，因此不能允许默认的过期宽限期重新放行令牌。
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()),
                new JwtTimestampValidator(Duration.ZERO)));
        return token -> {
            var jwt = decoder.decode(token);
            if (jwt.getExpiresAt() == null || !Instant.now().isBefore(jwt.getExpiresAt()))
                throw new BadJwtException("Access Token 已过期或缺少过期时间");
            try {
                if (denylist.isRevoked(token))
                    throw new BadJwtException("Access Token 已注销");
            } catch (AuthStateUnavailableException exception) {
                throw new JwtException("认证状态服务暂不可用", exception);
            }
            return jwt;
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper,
            LoginProtection protection) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, exception) -> {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof AuthStateUnavailableException) {
                    LOG.error("Authentication state is unavailable: method={} path={}",
                            request.getMethod(), request.getRequestURI(), exception);
                    writeError(response, objectMapper, 503, "AUTH_STATE_UNAVAILABLE", "认证服务暂不可用");
                    return;
                }
            }
            LOG.warn("Authentication failed: method={} path={} exceptionType={}",
                    request.getMethod(), request.getRequestURI(), exception.getClass().getSimpleName());
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            writeError(response, objectMapper, 401, "AUTHENTICATION_FAILED", "账号或凭证无效");
        };
        AccessDeniedHandler forbidden = (request, response, exception) -> {
            LOG.warn("Access denied: method={} path={} exceptionType={}",
                    request.getMethod(), request.getRequestURI(), exception.getClass().getSimpleName());
            writeError(response, objectMapper, 403, "ACCESS_DENIED", "无权访问该资源");
        };
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter authentication = new JwtAuthenticationConverter();
        authentication.setJwtGrantedAuthoritiesConverter(authorities);

        http.addFilterBefore(new LoginIpRateLimitFilter(protection, objectMapper), BearerTokenAuthenticationFilter.class)
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register", "/api/v1/auth/password/login",
                                "/api/v1/auth/admin/password/login",
                                "/api/v1/auth/token/refresh", "/api/v1/auth/logout",
                                "/api/v1/auth/passkeys/authentication/options",
                                "/api/v1/auth/passkeys/authentication/verify",
                                "/api/v1/auth/passkeys/authentication/admin/options",
                                "/api/v1/auth/passkeys/authentication/admin/verify").permitAll()
                        .requestMatchers(HttpMethod.GET, "/.well-known/assetlinks.json", "/actuator/health",
                                "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
                                "/doc.html", "/webjars/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authentication))
                        .authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AuthProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(properties.webauthn().allowedOrigins().stream()
                .filter(origin -> origin.startsWith("http://") || origin.startsWith("https://")).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        cors.setExposedHeaders(List.of(HttpHeaders.WWW_AUTHENTICATE));
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    private static void writeError(HttpServletResponse response, ObjectMapper mapper, int status,
            String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        mapper.writeValue(response.getOutputStream(), AuthErrorResponse.of(code, message));
    }
}
