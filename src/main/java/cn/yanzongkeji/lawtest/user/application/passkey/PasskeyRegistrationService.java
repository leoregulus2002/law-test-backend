package cn.yanzongkeji.lawtest.user.application.passkey;

import cn.yanzongkeji.lawtest.user.application.exception.PasskeyConflictException;
import cn.yanzongkeji.lawtest.user.domain.model.AuthCeremony;
import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import cn.yanzongkeji.lawtest.user.domain.port.AuthCeremonyRepository;
import cn.yanzongkeji.lawtest.user.domain.port.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialCreationOptionsRequest;
import org.springframework.security.web.webauthn.management.ImmutableRelyingPartyRegistrationRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyPublicKey;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasskeyRegistrationService implements PasskeyRegistrationUseCase {
    private static final WebAuthnCeremonyOptionsCodec OPTIONS_CODEC = new WebAuthnCeremonyOptionsCodec();

    private final UserRepository users;
    private final AuthCeremonyRepository ceremonies;
    private final WebAuthnRelyingPartyOperations relyingParty;

    public PasskeyRegistrationService(UserRepository users, AuthCeremonyRepository ceremonies,
            WebAuthnRelyingPartyOperations relyingParty) {
        this.users = users;
        this.ceremonies = ceremonies;
        this.relyingParty = relyingParty;
    }

    @Override
    @Transactional
    public PasskeyOptions<PublicKeyCredentialCreationOptions> beginRegistration(UserId userId) {
        UserAccount user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.username(), "webauthn-registration", AuthorityUtils.NO_AUTHORITIES);
        PublicKeyCredentialCreationOptions options = relyingParty.createPublicKeyCredentialCreationOptions(
                new ImmutablePublicKeyCredentialCreationOptionsRequest(authentication));
        Instant now = Instant.now();
        UUID ceremonyId = UUID.randomUUID();
        ceremonies.create(new AuthCeremony(ceremonyId, options.getChallenge().getBytes(),
                AuthCeremony.CeremonyType.REGISTER, user.id(), write(options), false, now,
                now.plusSeconds(300), null));
        return new PasskeyOptions<>(ceremonyId, options);
    }

    @Override
    @Transactional
    public void finishRegistration(UserId userId, UUID ceremonyId, String label,
            PublicKeyCredential<AuthenticatorAttestationResponse> credential) {
        AuthCeremony ceremony = ceremonies.consume(ceremonyId, AuthCeremony.CeremonyType.REGISTER, Instant.now());
        if (!userId.equals(ceremony.userId())) {
            throw new IllegalArgumentException("认证操作不属于当前用户");
        }
        String safeLabel = validateLabel(label);
        try {
            relyingParty.registerCredential(new ImmutableRelyingPartyRegistrationRequest(
                    read(ceremony.optionsJson(), PublicKeyCredentialCreationOptions.class),
                    new RelyingPartyPublicKey(credential, safeLabel)));
        } catch (DuplicateKeyException exception) {
            throw new PasskeyConflictException();
        } catch (IllegalArgumentException exception) {
            if (isDuplicate(exception)) {
                throw new PasskeyConflictException();
            }
            throw exception;
        }
    }

    private String write(PublicKeyCredentialCreationOptions options) {
        return OPTIONS_CODEC.write(options);
    }

    private <T> T read(String encoded, Class<T> type) {
        return OPTIONS_CODEC.read(encoded, type);
    }

    private static String validateLabel(String label) {
        if (label == null) {
            throw new IllegalArgumentException("Passkey 名称不能为空");
        }
        String value = label.strip();
        int length = value.codePointCount(0, value.length());
        if (length < 1 || length > 64) {
            throw new IllegalArgumentException("Passkey 名称必须为 1–64 个字符");
        }
        return value;
    }

    private static boolean isDuplicate(IllegalArgumentException exception) {
        String message = exception.getMessage();
        return message != null && message.toLowerCase(java.util.Locale.ROOT).contains("already exists");
    }
}
