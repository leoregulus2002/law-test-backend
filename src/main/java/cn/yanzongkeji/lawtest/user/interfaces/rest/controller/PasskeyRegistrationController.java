package cn.yanzongkeji.lawtest.user.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.user.application.passkey.PasskeyRegistrationUseCase;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.interfaces.rest.request.PasskeyRegistrationVerifyRequest;
import cn.yanzongkeji.lawtest.user.interfaces.rest.response.PasskeyOptionsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/passkeys/registration")
@Tag(name = "Passkey注册与管理", description = "Passkey 注册与管理")
public class PasskeyRegistrationController {
    private final PasskeyRegistrationUseCase passkeys;

    @PostMapping("/options")
    @Operation(summary = "创建 Passkey 注册选项")
    public ResponseEntity<PasskeyOptionsResponse<?>> options(JwtAuthenticationToken authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PasskeyOptionsResponse.from(
                passkeys.beginRegistration(currentUser(authentication))));
    }

    @PostMapping("/verify")
    @Operation(summary = "验证并保存 Passkey")
    public ResponseEntity<Void> verify(JwtAuthenticationToken authentication,
            @RequestBody PasskeyRegistrationVerifyRequest request) {
        passkeys.finishRegistration(currentUser(authentication), request.ceremonyId(), request.label(), request.publicKey());
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).build();
    }

    public static UserId currentUser(JwtAuthenticationToken authentication) {
        try {
            return new UserId(Long.parseLong(authentication.getToken().getSubject()));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("无效的用户身份");
        }
    }
}
