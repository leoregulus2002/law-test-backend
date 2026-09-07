package cn.yanzongkeji.lawtest.user.application.auth;

public interface AuthUseCase {
    TokenPair register(String username, String password, String displayName);

    TokenPair passwordLogin(String username, String password);

    TokenPair refresh(String refreshToken);

    void logout(String refreshToken);
}
