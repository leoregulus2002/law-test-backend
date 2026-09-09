package cn.yanzongkeji.lawtest.user.application.passkey;

import cn.yanzongkeji.lawtest.user.application.auth.TokenPair;
import java.util.UUID;
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;

public interface PasskeyAuthenticationUseCase {
    PasskeyOptions<PublicKeyCredentialRequestOptions> beginAuthentication(String username);

    PasskeyOptions<PublicKeyCredentialRequestOptions> beginAdminAuthentication(String username);

    TokenPair finishAuthentication(UUID ceremonyId,
            PublicKeyCredential<AuthenticatorAssertionResponse> credential);

    TokenPair finishAdminAuthentication(UUID ceremonyId,
            PublicKeyCredential<AuthenticatorAssertionResponse> credential);
}
