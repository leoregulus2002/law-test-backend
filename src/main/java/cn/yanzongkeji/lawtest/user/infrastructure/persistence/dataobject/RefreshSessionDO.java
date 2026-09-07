package cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.ToString;

@Data
@TableName("refresh_session")
public class RefreshSessionDO {
    @TableId(type = IdType.INPUT)
    private UUID id;
    private Long userId;
    @ToString.Exclude
    private byte[] tokenHash;
    private UUID tokenFamilyId;
    private UUID previousSessionId;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant usedAt;
    private Instant revokedAt;

    public byte[] getTokenHash() {
        return tokenHash == null ? null : tokenHash.clone();
    }

    public void setTokenHash(byte[] tokenHash) {
        this.tokenHash = tokenHash == null ? null : tokenHash.clone();
    }
}
