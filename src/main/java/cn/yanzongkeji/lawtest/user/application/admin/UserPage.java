package cn.yanzongkeji.lawtest.user.application.admin;

import cn.yanzongkeji.lawtest.user.domain.model.UserAccount;
import java.util.List;

public record UserPage(List<UserAccount> items, int page, int size, long total) {
    public UserPage {
        items = List.copyOf(items);
    }
}
