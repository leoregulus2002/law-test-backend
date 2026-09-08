-- 切换后旧的数据库刷新会话失效，用户需要重新登录。
drop table refresh_session;

alter table app_user
    drop column failed_login_attempts,
    drop column locked_until;
