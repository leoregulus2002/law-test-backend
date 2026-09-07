package cn.yanzongkeji.lawtest.user.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.user.application.passkey.PasskeyManagementUseCase;
import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.port.UserRepository;
import cn.yanzongkeji.lawtest.user.interfaces.rest.response.CurrentUserResponse;
import cn.yanzongkeji.lawtest.user.interfaces.rest.response.PasskeyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "用户", description = "当前用户与 Passkey 管理")
public class UserController {
    private final UserRepository users;
    private final PasskeyManagementUseCase passkeys;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户")
    public ResponseEntity<CurrentUserResponse> currentUser(JwtAuthenticationToken authentication) {
        UserAccount user = users.findById(currentUserId(authentication))
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(CurrentUserResponse.from(user));
    }

    @GetMapping("/me/passkeys")
    @Operation(summary = "列出当前用户的 Passkey")
    public ResponseEntity<List<PasskeyResponse>> listPasskeys(JwtAuthenticationToken authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(passkeys.list(currentUserId(authentication))
                .stream().map(PasskeyResponse::from).toList());
    }

    @DeleteMapping("/me/passkeys/{credentialId}")
    @Operation(summary = "删除当前用户的 Passkey")
    public ResponseEntity<Void> deletePasskey(JwtAuthenticationToken authentication,
            @PathVariable String credentialId) {
        passkeys.delete(currentUserId(authentication), credentialId);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    private static UserId currentUserId(JwtAuthenticationToken authentication) {
        return PasskeyRegistrationController.currentUser(authentication);
    }
}
