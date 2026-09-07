package cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;
import lombok.ToString;

@Data
@TableName("app_user")
public class AppUserDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @TableField("display_name")
    private String displayName;
    @ToString.Exclude
    @TableField("password_hash")
    private String passwordHash;
    @ToString.Exclude
    @TableField("webauthn_user_handle")
    private byte[] webauthnUserHandle;
    private String status;
    @TableField("failed_login_attempts")
    private Integer failedLoginAttempts;
    @TableField(value = "locked_until", updateStrategy = FieldStrategy.ALWAYS)
    private Instant lockedUntil;
    @TableField("created_at")
    private Instant createdAt;
    @TableField("updated_at")
    private Instant updatedAt;
}
