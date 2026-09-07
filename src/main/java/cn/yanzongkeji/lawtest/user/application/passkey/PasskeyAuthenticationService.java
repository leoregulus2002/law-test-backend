package cn.yanzongkeji.lawtest.user.application.passkey;

import cn.yanzongkeji.lawtest.user.application.auth.TokenPair;
import cn.yanzongkeji.lawtest.user.application.auth.TokenSessionService;
import cn.yanzongkeji.lawtest.user.application.exception.AuthenticationFailedException;
import cn.yanzongkeji.lawtest.user.domain.model.AuthCeremony;
import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.port.AuthCeremonyRepository;
import cn.yanzongkeji.lawtest.user.domain.port.UserRepository;
import cn.yanzongkeji.lawtest.user.infrastructure.configuration.AuthProperties;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialDescriptor;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.security.web.webauthn.api.UserVerificationRequirement;
import org.springframework.security.web.webauthn.management.ImmutablePublicKeyCredentialRequestOptionsRequest;
import org.springframework.security.web.webauthn.management.RelyingPartyAuthenticationRequest;
import org.springframework.security.web.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class PasskeyAuthenticationService implements PasskeyAuthenticationUseCase {
    private static final Logger LOG = LoggerFactory.getLogger(PasskeyAuthenticationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final AuthCeremonyRepository ceremonies;
    private final WebAuthnRelyingPartyOperations relyingParty;
    private final TokenSessionService tokens;
    private final ObjectMapper objectMapper;
    private final AuthProperties.WebAuthn webauthn;

    public PasskeyAuthenticationService(UserRepository users, AuthCeremonyRepository ceremonies,
            WebAuthnRelyingPartyOperations relyingParty, TokenSessionService tokens, ObjectMapper objectMapper,
            AuthProperties properties) {
        this.users = users;
        this.ceremonies = ceremonies;
        this.relyingParty = relyingParty;
        this.tokens = tokens;
        this.objectMapper = objectMapper;
        this.webauthn = properties.webauthn();
    }

    @Override
    @Transactional
    public PasskeyOptions<PublicKeyCredentialRequestOptions> beginAuthentication(String username) {
        UserAccount user = findEligibleUser(username);
        boolean dummy = user == null;
        PublicKeyCredentialRequestOptions options = dummy ? dummyOptions() : requestOptions(user);
        Instant now = Instant.now();
        UUID ceremonyId = UUID.randomUUID();
        ceremonies.create(new AuthCeremony(ceremonyId, options.getChallenge().getBytes(),
                AuthCeremony.CeremonyType.AUTHENTICATE, user == null ? null : user.id(), write(options), dummy,
                now, now.plusSeconds(300), null));
        return new PasskeyOptions<>(ceremonyId, options);
    }

    @Override
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public TokenPair finishAuthentication(UUID ceremonyId,
            PublicKeyCredential<AuthenticatorAssertionResponse> credential) {
        AuthCeremony ceremony = ceremonies.consume(ceremonyId, AuthCeremony.CeremonyType.AUTHENTICATE, Instant.now());
        if (ceremony.dummy() || ceremony.userId() == null) {
            throw new AuthenticationFailedException();
        }
        try {
            var entity = relyingParty.authenticate(new RelyingPartyAuthenticationRequest(
                    read(ceremony.optionsJson(), PublicKeyCredentialRequestOptions.class), credential));
            UserAccount user = users.findById(ceremony.userId()).orElseThrow(AuthenticationFailedException::new);
            if (entity == null || !java.util.Arrays.equals(entity.getId().getBytes(), user.webauthnUserHandle())) {
                throw new AuthenticationFailedException();
            }
            if (!user.isPasswordLoginAllowed(Instant.now())) {
                throw new AuthenticationFailedException();
            }
            users.clearPasswordFailures(user.id());
            return tokens.issue(user);
        } catch (AuthenticationFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOG.warn("Passkey authentication failed category={} ceremonyId={}",
                    exception.getClass().getSimpleName(), ceremonyId);
            throw new AuthenticationFailedException();
        }
    }

    private UserAccount findEligibleUser(String username) {
        try {
            UserAccount user = users.findByUsername(UserAccount.normalizeUsername(username)).orElse(null);
            if (user == null || !user.isPasswordLoginAllowed(Instant.now())) {
                return null;
            }
            // The adapter only returns this user's credentials, so an empty allow-list is safe to
            // treat as a dummy ceremony and prevents account state disclosure.
            PublicKeyCredentialRequestOptions options = requestOptions(user);
            return options.getAllowCredentials().isEmpty() ? null : user;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private PublicKeyCredentialRequestOptions requestOptions(UserAccount user) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.username(), "webauthn-authentication", AuthorityUtils.NO_AUTHORITIES);
        return relyingParty.createCredentialRequestOptions(
                new ImmutablePublicKeyCredentialRequestOptionsRequest(authentication));
    }

    private PublicKeyCredentialRequestOptions dummyOptions() {
        byte[] challenge = new byte[32];
        byte[] credentialId = new byte[32];
        RANDOM.nextBytes(challenge);
        RANDOM.nextBytes(credentialId);
        return PublicKeyCredentialRequestOptions.builder().challenge(new Bytes(challenge))
                .timeout(webauthn.ceremonyTtl()).rpId(webauthn.rpId())
                .allowCredentials(List.of(PublicKeyCredentialDescriptor.builder().id(new Bytes(credentialId)).build()))
                .userVerification(UserVerificationRequirement.REQUIRED).build();
    }

    private String write(Object options) {
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JacksonException exception) {
            throw new IllegalStateException("无法保存 WebAuthn ceremony", exception);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JacksonException exception) {
            throw new AuthenticationFailedException();
        }
    }
}
