create table user_daily_question (
    user_id bigint not null,
    study_date date not null,
    question_id bigint not null,
    constraint user_daily_question_primary_key primary key (user_id, study_date, question_id),
    constraint user_daily_question_user_id_fkey
        foreign key (user_id) references app_user (id) on delete cascade,
    constraint user_daily_question_question_id_fkey
        foreign key (question_id) references question (id) on delete cascade
);

create index user_daily_question_user_date_idx on user_daily_question (user_id, study_date desc);
