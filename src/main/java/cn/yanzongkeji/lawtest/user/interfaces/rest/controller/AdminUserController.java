package cn.yanzongkeji.lawtest.user.interfaces.rest.controller;

import cn.yanzongkeji.lawtest.question.interfaces.rest.response.PageResponse;
import cn.yanzongkeji.lawtest.user.application.admin.AdminUserManagementUseCase;
import cn.yanzongkeji.lawtest.user.application.admin.UserPage;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.model.UserRole;
import cn.yanzongkeji.lawtest.user.domain.model.UserStatus;
import cn.yanzongkeji.lawtest.user.interfaces.rest.request.AdminPasswordResetRequest;
import cn.yanzongkeji.lawtest.user.interfaces.rest.request.AdminUserCreateRequest;
import cn.yanzongkeji.lawtest.user.interfaces.rest.request.AdminUserUpdateRequest;
import cn.yanzongkeji.lawtest.user.interfaces.rest.response.AdminUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
@Tag(name = "系统用户管理", description = "仅管理员可管理普通用户和管理员账户")
public class AdminUserController {
    private final AdminUserManagementUseCase users;

    @GetMapping
    @Operation(summary = "分页查询系统用户")
    public PageResponse<AdminUserResponse> list(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role, @RequestParam(required = false) UserStatus status) {
        UserPage result = users.list(page, size, keyword, role, status);
        return new PageResponse<>(result.items().stream().map(AdminUserResponse::from).toList(), result.page(),
                result.size(), result.total());
    }

    @PostMapping
    @Operation(summary = "创建系统用户")
    public ResponseEntity<AdminUserResponse> create(@RequestBody AdminUserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminUserResponse.from(
                users.create(request.username(), request.password(), request.displayName(), request.role())));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "修改系统用户资料、角色和状态")
    public AdminUserResponse update(@PathVariable long id, @RequestBody AdminUserUpdateRequest request,
            JwtAuthenticationToken authentication) {
        return AdminUserResponse.from(users.update(new UserId(id), request.displayName(), request.role(),
                request.status(), currentUser(authentication)));
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "重置系统用户密码")
    public ResponseEntity<Void> resetPassword(@PathVariable long id, @RequestBody AdminPasswordResetRequest request,
            JwtAuthenticationToken authentication) {
        users.resetPassword(new UserId(id), request.password(), currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除系统用户")
    public ResponseEntity<Void> delete(@PathVariable long id, JwtAuthenticationToken authentication) {
        users.delete(new UserId(id), currentUser(authentication));
        return ResponseEntity.noContent().build();
    }

    private static UserId currentUser(JwtAuthenticationToken authentication) {
        return PasskeyRegistrationController.currentUser(authentication);
    }
}
