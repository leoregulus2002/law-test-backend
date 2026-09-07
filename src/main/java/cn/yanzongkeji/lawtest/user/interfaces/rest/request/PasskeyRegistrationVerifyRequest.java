package cn.yanzongkeji.lawtest.user.interfaces.rest.request;

import java.util.UUID;
import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;

public record PasskeyRegistrationVerifyRequest(UUID ceremonyId, String label,
        PublicKeyCredential<AuthenticatorAttestationResponse> publicKey) {
}
