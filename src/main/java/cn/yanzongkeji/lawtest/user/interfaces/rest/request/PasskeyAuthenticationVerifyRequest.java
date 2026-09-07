package cn.yanzongkeji.lawtest.user.interfaces.rest.request;

import java.util.UUID;
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;

public record PasskeyAuthenticationVerifyRequest(UUID ceremonyId,
        PublicKeyCredential<AuthenticatorAssertionResponse> publicKey) {
}
