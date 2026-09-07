package cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Data;
import lombok.ToString;

@Data
@TableName("user_passkey")
public class UserPasskeyDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String credentialType;
    @ToString.Exclude
    private byte[] credentialId;
    @ToString.Exclude
    private byte[] cosePublicKey;
    private Long signatureCount;
    private Boolean userVerified;
    private String transports;
    private Boolean backupEligible;
    private Boolean backupState;
    @ToString.Exclude
    private byte[] aaguid;
    @ToString.Exclude
    private byte[] attestationObject;
    @ToString.Exclude
    private byte[] clientDataJson;
    private String label;
    private Instant createdAt;
    private Instant lastUsedAt;

    public byte[] getCredentialId() { return copy(credentialId); }
    public void setCredentialId(byte[] value) { credentialId = copy(value); }
    public byte[] getCosePublicKey() { return copy(cosePublicKey); }
    public void setCosePublicKey(byte[] value) { cosePublicKey = copy(value); }
    public byte[] getAaguid() { return copy(aaguid); }
    public void setAaguid(byte[] value) { aaguid = copy(value); }
    public byte[] getAttestationObject() { return copy(attestationObject); }
    public void setAttestationObject(byte[] value) { attestationObject = copy(value); }
    public byte[] getClientDataJson() { return copy(clientDataJson); }
    public void setClientDataJson(byte[] value) { clientDataJson = copy(value); }

    private static byte[] copy(byte[] value) {
        return value == null ? null : value.clone();
    }
}
