create table question_bank (
    id bigint generated always as identity primary key,
    code varchar(64) not null,
    name varchar(128) not null,
    source_file_name varchar(255),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint question_bank_code_unique unique (code)
);

create table question (
    id bigint generated always as identity primary key,
    question_bank_id bigint not null,
    sequence_no integer not null,
    stem text not null,
    analysis text not null default '',
    question_type varchar(32) not null default 'MULTIPLE_CHOICE',
    source_payload jsonb not null default '{}'::jsonb,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint question_sequence_no_positive check (sequence_no > 0),
    constraint question_question_bank_id_fkey
        foreign key (question_bank_id) references question_bank (id) on delete cascade,
    constraint question_bank_sequence_unique unique (question_bank_id, sequence_no)
);

create index question_question_bank_id_idx on question (question_bank_id);

create table question_option (
    id bigint generated always as identity primary key,
    question_id bigint not null,
    label varchar(1) not null,
    content text not null,
    display_order smallint not null,
    constraint question_option_label_valid check (label ~ '^[A-Z]$'),
    constraint question_option_display_order_positive check (display_order > 0),
    constraint question_option_question_id_fkey
        foreign key (question_id) references question (id) on delete cascade,
    constraint question_option_question_label_unique unique (question_id, label),
    constraint question_option_question_order_unique unique (question_id, display_order)
);

create index question_option_question_id_idx on question_option (question_id);

create table question_answer (
    question_id bigint not null,
    option_label varchar(1) not null,
    constraint question_answer_primary_key primary key (question_id, option_label),
    constraint question_answer_question_option_fkey
        foreign key (question_id, option_label)
        references question_option (question_id, label) on delete cascade
);
