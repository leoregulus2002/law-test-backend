package cn.yanzongkeji.lawtest.user.infrastructure.security;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import cn.yanzongkeji.lawtest.user.domain.port.AccessTokenIssuer;
import cn.yanzongkeji.lawtest.user.infrastructure.configuration.AuthProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {
    private final AuthProperties.Jwt properties;
    private final JwtEncoder encoder;

    public JwtAccessTokenIssuer(AuthProperties properties) {
        this.properties = properties.jwt();
        byte[] secret = this.properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32)
            throw new IllegalArgumentException("JWT 签名密钥至少需要 32 个 UTF-8 字节");
        if (this.properties.accessTokenTtl().isNegative() || this.properties.accessTokenTtl().isZero())
            throw new IllegalArgumentException("Access Token 有效期必须为正数");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret));
    }

    @Override
    public String issue(UserAccount user, Instant issuedAt) {
        Objects.requireNonNull(user.id(), "签发令牌前必须保存用户");
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(Long.toString(user.id().value()))
                .claim("username", user.username())
                .claim("roles", List.of(user.role().name()))
                .issuer(properties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
