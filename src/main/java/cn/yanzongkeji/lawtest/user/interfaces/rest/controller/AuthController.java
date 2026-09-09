package cn.yanzongkeji.lawtest.user.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.user.application.auth.AuthUseCase;
import cn.yanzongkeji.lawtest.user.domain.port.AccessTokenDenylist;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import cn.yanzongkeji.lawtest.user.interfaces.rest.request.PasswordLoginRequest;
import cn.yanzongkeji.lawtest.user.interfaces.rest.request.RefreshTokenRequest;
import cn.yanzongkeji.lawtest.user.interfaces.rest.request.RegisterRequest;
import cn.yanzongkeji.lawtest.user.interfaces.rest.response.TokenPairResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "用户认证", description = "注册、密码登录与令牌管理")
public class AuthController {
    private final AuthUseCase auth;
    private final AccessTokenDenylist denylist;

    @PostMapping("/register")
    @Operation(summary = "自助注册并登录")
    public ResponseEntity<TokenPairResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(TokenPairResponse.from(
                auth.register(request.username(), request.password(), request.displayName())));
    }

    @PostMapping("/password/login")
    @Operation(summary = "账号密码登录")
    public ResponseEntity<TokenPairResponse> passwordLogin(@RequestBody PasswordLoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(TokenPairResponse.from(
                auth.passwordLogin(request.username(), request.password())));
    }

    @PostMapping("/admin/password/login")
    @Operation(summary = "管理员账号密码登录")
    public ResponseEntity<TokenPairResponse> adminPasswordLogin(@RequestBody PasswordLoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(TokenPairResponse.from(
                auth.adminPasswordLogin(request.username(), request.password())));
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "轮换刷新令牌")
    public ResponseEntity<TokenPairResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(TokenPairResponse.from(
                auth.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    @Operation(summary = "撤销刷新令牌家族及携带的 Access Token")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        auth.logout(request.refreshToken());
        if (jwt != null)
            denylist.revoke(jwt.getTokenValue(), jwt.getExpiresAt());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
