create table user_favorite_question (
    user_id bigint not null,
    question_id bigint not null,
    created_at timestamp with time zone not null default current_timestamp,
    constraint user_favorite_question_primary_key primary key (user_id, question_id),
    constraint user_favorite_question_user_id_fkey
        foreign key (user_id) references app_user (id) on delete cascade,
    constraint user_favorite_question_question_id_fkey
        foreign key (question_id) references question (id) on delete cascade
);

create index user_favorite_question_user_created_idx
    on user_favorite_question (user_id, created_at desc, question_id desc);
