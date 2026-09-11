create table high_frequency_topic (
    id bigint generated always as identity primary key,
    title varchar(128) not null,
    summary text not null,
    category varchar(64) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create index high_frequency_topic_created_at_idx on high_frequency_topic (created_at desc);
