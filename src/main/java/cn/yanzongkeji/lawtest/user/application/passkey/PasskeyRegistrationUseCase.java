package cn.yanzongkeji.lawtest.user.application.passkey;

import cn.yanzongkeji.lawtest.user.domain.model.UserId;
import java.util.UUID;
import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;

public interface PasskeyRegistrationUseCase {
    PasskeyOptions<PublicKeyCredentialCreationOptions> beginRegistration(UserId userId);

    void finishRegistration(UserId userId, UUID ceremonyId, String label,
            PublicKeyCredential<AuthenticatorAttestationResponse> credential);
}
