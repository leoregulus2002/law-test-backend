drop table if exists ai_grading_configuration;

create table system_configuration (
    config_key varchar(64) primary key,
    config_value text not null,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint system_configuration_key_valid check (config_key = lower(config_key))
);

insert into system_configuration (config_key, config_value)
values (
    'openapi',
    '{"enabled":false,"baseUrl":"","apiKey":"","model":"","timeoutSeconds":45}'
);
