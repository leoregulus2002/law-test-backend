package cn.yanzongkeji.lawtest.user.infrastructure.redis;

import cn.yanzongkeji.lawtest.user.application.exception.AuthStateUnavailableException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.script.DefaultRedisScript;

final class RedisAuthSupport {
    private RedisAuthSupport() {}

    static <T> DefaultRedisScript<T> script(String name, Class<T> resultType) {
        DefaultRedisScript<T> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/" + name + ".lua"));
        script.setResultType(resultType);
        return script;
    }

    static <T> T required(Supplier<T> operation) {
        try {
            T result = operation.get();
            if (result == null)
                throw new AuthStateUnavailableException(new IllegalStateException("Redis 未返回结果"));
            return result;
        } catch (DataAccessException exception) {
            throw new AuthStateUnavailableException(exception);
        }
    }

    static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
