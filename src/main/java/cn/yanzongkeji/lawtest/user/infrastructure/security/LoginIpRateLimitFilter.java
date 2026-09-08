package cn.yanzongkeji.lawtest.user.infrastructure.security;

import cn.yanzongkeji.lawtest.user.application.exception.AuthStateUnavailableException;
import cn.yanzongkeji.lawtest.user.application.exception.LoginRateLimitedException;
import cn.yanzongkeji.lawtest.user.domain.port.LoginProtection;
import cn.yanzongkeji.lawtest.user.interfaces.rest.response.AuthErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

final class LoginIpRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> PATHS = Set.of(
            "/api/v1/auth/password/login",
            "/api/v1/auth/passkeys/authentication/options",
            "/api/v1/auth/passkeys/authentication/verify");
    private final LoginProtection protection;
    private final ObjectMapper mapper;

    LoginIpRateLimitFilter(LoginProtection protection, ObjectMapper mapper) {
        this.protection = protection;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        try {
            // 不直接信任客户端提供的 X-Forwarded-For；代理应在容器可信代理配置中处理。
            protection.checkIpRateLimit(request.getRemoteAddr());
        } catch (LoginRateLimitedException exception) {
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()));
            writeError(response, 429, "LOGIN_RATE_LIMITED", "登录请求过于频繁");
            return;
        } catch (AuthStateUnavailableException exception) {
            writeError(response, 503, "AUTH_STATE_UNAVAILABLE", "认证服务暂不可用");
            return;
        }
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        mapper.writeValue(response.getOutputStream(), AuthErrorResponse.of(code, message));
    }
}
