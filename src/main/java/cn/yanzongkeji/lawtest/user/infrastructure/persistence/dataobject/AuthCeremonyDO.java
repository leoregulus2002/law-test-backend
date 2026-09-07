package cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.ToString;

@Data
@TableName("auth_ceremony")
public class AuthCeremonyDO {
    @TableId(type = IdType.INPUT)
    private UUID id;
    @ToString.Exclude
    private byte[] challenge;
    private String ceremonyType;
    private Long userId;
    @ToString.Exclude
    private String options;
    private Boolean dummy;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant consumedAt;

    public byte[] getChallenge() {
        return challenge == null ? null : challenge.clone();
    }

    public void setChallenge(byte[] challenge) {
        this.challenge = challenge == null ? null : challenge.clone();
    }
}
