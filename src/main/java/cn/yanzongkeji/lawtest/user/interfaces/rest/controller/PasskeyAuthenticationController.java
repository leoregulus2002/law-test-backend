package cn.yanzongkeji.lawtest.user.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.user.application.passkey.PasskeyAuthenticationUseCase;
import cn.yanzongkeji.lawtest.user.interfaces.rest.request.PasskeyAuthenticationOptionsRequest;
import cn.yanzongkeji.lawtest.user.interfaces.rest.request.PasskeyAuthenticationVerifyRequest;
import cn.yanzongkeji.lawtest.user.interfaces.rest.response.PasskeyOptionsResponse;
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
@RequestMapping("/api/v1/auth/passkeys/authentication")
@Tag(name = "Passkey登录", description = "账号范围 Passkey 登录")
public class PasskeyAuthenticationController {
    private final PasskeyAuthenticationUseCase passkeys;

    @PostMapping("/options")
    @Operation(summary = "创建账号范围 Passkey 登录选项")
    public ResponseEntity<PasskeyOptionsResponse<?>> options(@RequestBody PasskeyAuthenticationOptionsRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PasskeyOptionsResponse.from(
                passkeys.beginAuthentication(request.username())));
    }

    @PostMapping("/verify")
    @Operation(summary = "验证 Passkey 登录")
    public ResponseEntity<TokenPairResponse> verify(@RequestBody PasskeyAuthenticationVerifyRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(TokenPairResponse.from(
                passkeys.finishAuthentication(request.ceremonyId(), request.publicKey())));
    }
}
