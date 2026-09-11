create table mock_exam (
    id bigint generated always as identity primary key,
    title varchar(128) not null,
    duration_minutes integer not null,
    passing_score numeric(8,2) not null,
    status varchar(16) not null default 'DRAFT',
    created_at timestamp with time zone not null default current_timestamp,
    published_at timestamp with time zone,
    constraint mock_exam_duration_valid check (duration_minutes > 0),
    constraint mock_exam_passing_score_valid check (passing_score >= 0),
    constraint mock_exam_status_valid check (status in ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

create table mock_exam_bank (
    exam_id bigint not null references mock_exam (id) on delete cascade,
    question_bank_id bigint not null references question_bank (id),
    constraint mock_exam_bank_primary_key primary key (exam_id, question_bank_id)
);

create table mock_exam_rule (
    id bigint generated always as identity primary key,
    exam_id bigint not null references mock_exam (id) on delete cascade,
    question_type varchar(32) not null,
    selection_mode varchar(16) not null,
    question_count integer not null,
    score numeric(8,2) not null,
    constraint mock_exam_rule_type_valid check (question_type in ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'INDETERMINATE_CHOICE', 'SUBJECTIVE')),
    constraint mock_exam_rule_selection_valid check (selection_mode in ('RANDOM', 'ALL')),
    constraint mock_exam_rule_count_valid check (question_count >= 0),
    constraint mock_exam_rule_score_valid check (score > 0),
    constraint mock_exam_rule_unique unique (exam_id, question_type)
);

create table mock_exam_attempt (
    id bigint generated always as identity primary key,
    exam_id bigint not null references mock_exam (id),
    user_id bigint not null references app_user (id) on delete cascade,
    started_at timestamp with time zone not null default current_timestamp,
    expires_at timestamp with time zone not null,
    submitted_at timestamp with time zone,
    status varchar(16) not null default 'IN_PROGRESS',
    score numeric(8,2),
    passed boolean,
    constraint mock_exam_attempt_status_valid check (status in ('IN_PROGRESS', 'SUBMITTED'))
);

create index mock_exam_attempt_user_exam_idx on mock_exam_attempt (user_id, exam_id, id desc);

create table mock_exam_attempt_question (
    id bigint generated always as identity primary key,
    attempt_id bigint not null references mock_exam_attempt (id) on delete cascade,
    question_id bigint not null references question (id),
    order_no integer not null,
    question_type varchar(32) not null,
    score numeric(8,2) not null,
    correct_answer text not null default '',
    reference_answer text not null default '',
    selected_answer text not null default '',
    subjective_answer text not null default '',
    awarded_score numeric(8,2),
    constraint mock_exam_attempt_question_order_unique unique (attempt_id, order_no)
);

create index mock_exam_attempt_question_attempt_idx on mock_exam_attempt_question (attempt_id, order_no);
