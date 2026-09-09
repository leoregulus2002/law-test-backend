create table user_question_progress (
    user_id bigint not null,
    question_id bigint not null,
    status varchar(16) not null,
    answered_at timestamp with time zone not null default current_timestamp,
    constraint user_question_progress_primary_key primary key (user_id, question_id),
    constraint user_question_progress_user_id_fkey
        foreign key (user_id) references app_user (id) on delete cascade,
    constraint user_question_progress_question_id_fkey
        foreign key (question_id) references question (id) on delete cascade,
    constraint user_question_progress_status_valid check (status in ('ANSWERED', 'WRONG'))
);

create index user_question_progress_user_status_idx on user_question_progress (user_id, status, question_id);

create table user_sequential_progress (
    user_id bigint not null,
    scope_key varchar(64) not null,
    question_id bigint not null,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint user_sequential_progress_primary_key primary key (user_id, scope_key),
    constraint user_sequential_progress_user_id_fkey
        foreign key (user_id) references app_user (id) on delete cascade,
    constraint user_sequential_progress_question_id_fkey
        foreign key (question_id) references question (id) on delete cascade,
    constraint user_sequential_progress_scope_key_valid check (scope_key = 'ALL' or scope_key ~ '^BANK:[1-9][0-9]*$')
);
