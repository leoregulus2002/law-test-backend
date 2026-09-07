package cn.yanzongkeji.lawtest.user.infrastructure.persistence.mapper;

import cn.yanzongkeji.lawtest.user.infrastructure.persistence.dataobject.UserPasskeyDO;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserPasskeyMapper {
    @Select("select * from user_passkey where credential_id = #{credentialId}")
    UserPasskeyDO selectByCredentialId(@Param("credentialId") byte[] credentialId);

    @Select("select * from user_passkey where user_id = #{userId} order by id")
    List<UserPasskeyDO> selectByUserId(@Param("userId") long userId);

    @Insert("""
            insert into user_passkey
                (user_id, credential_type, credential_id, cose_public_key, signature_count, user_verified,
                 transports, backup_eligible, backup_state, aaguid, attestation_object, client_data_json,
                 label, created_at, last_used_at)
            values
                (#{userId}, #{credentialType}, #{credentialId}, #{cosePublicKey}, #{signatureCount},
                 #{userVerified}, cast(#{transports} as jsonb), #{backupEligible}, #{backupState},
                 #{aaguid}, #{attestationObject}, #{clientDataJson}, #{label}, #{createdAt}, #{lastUsedAt})
            on conflict (credential_id) do update set
                user_id = excluded.user_id,
                credential_type = excluded.credential_type,
                cose_public_key = excluded.cose_public_key,
                signature_count = excluded.signature_count,
                user_verified = excluded.user_verified,
                transports = excluded.transports,
                backup_eligible = excluded.backup_eligible,
                backup_state = excluded.backup_state,
                aaguid = excluded.aaguid,
                attestation_object = excluded.attestation_object,
                client_data_json = excluded.client_data_json,
                label = excluded.label,
                created_at = excluded.created_at,
                last_used_at = excluded.last_used_at
            """)
    int upsert(UserPasskeyDO passkey);

    @Delete("delete from user_passkey where credential_id = #{credentialId}")
    int deleteByCredentialId(@Param("credentialId") byte[] credentialId);
}
