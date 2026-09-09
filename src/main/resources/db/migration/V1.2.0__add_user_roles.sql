alter table app_user add column role varchar(32) not null default 'USER';

alter table app_user add constraint app_user_role_valid check (role in ('USER', 'ADMIN'));

create index app_user_role_status_idx on app_user (role, status);
