package cn.yanzongkeji.lawtest.user.infrastructure.webauthn;

import cn.yanzongkeji.lawtest.user.infrastructure.configuration.AuthProperties;
import java.time.Duration;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.webauthn.api.AttestationConveyancePreference;
import org.springframework.security.web.webauthn.api.AuthenticatorSelectionCriteria;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRpEntity;
import org.springframework.security.web.webauthn.api.ResidentKeyRequirement;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.security.web.webauthn.management.Webauthn4JRelyingPartyOperations;

@Configuration(proxyBeanMethods = false)
public class WebAuthnConfiguration {
    private static final Duration CEREMONY_TIMEOUT = Duration.ofMinutes(5);

    @Bean
    WebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations(
            PostgresPublicKeyCredentialUserEntityRepository userEntities,
            PostgresUserCredentialRepository credentials, AuthProperties properties) {
        if (!CEREMONY_TIMEOUT.equals(properties.webauthn().ceremonyTtl())) {
            throw new IllegalStateException("WebAuthn ceremony TTL must be exactly five minutes");
        }
        PublicKeyCredentialRpEntity rpEntity = PublicKeyCredentialRpEntity.builder()
                .id(properties.webauthn().rpId())
                .name(properties.webauthn().rpName())
                .build();
        // WebAuthn4J's Origin accepts opaque custom schemes, so Android apk-key-hash origins
        // remain in the exact configured whitelist alongside HTTPS origins.
        Webauthn4JRelyingPartyOperations operations = new Webauthn4JRelyingPartyOperations(
                userEntities, credentials, rpEntity, Set.copyOf(properties.webauthn().allowedOrigins()));
        operations.setCustomizeCreationOptions(options -> options
                .timeout(CEREMONY_TIMEOUT)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .userVerification(UserVerificationRequirement.REQUIRED)
                        .build())
                .attestation(AttestationConveyancePreference.NONE));
        operations.setCustomizeRequestOptions(options -> options
                .timeout(CEREMONY_TIMEOUT)
                .userVerification(UserVerificationRequirement.REQUIRED));
        return operations;
    }
}
