create table ai_grading_configuration (
    id smallint primary key,
    enabled boolean not null default false,
    base_url text not null default '',
    api_key text not null default '',
    model varchar(128) not null default '',
    timeout_seconds integer not null default 45,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint ai_grading_configuration_singleton check (id = 1),
    constraint ai_grading_configuration_timeout_valid check (timeout_seconds between 5 and 120)
);

insert into ai_grading_configuration (id) values (1);
