alter table question
    add column status varchar(16) not null default 'ACTIVE',
    add constraint question_status_valid check (status in ('ACTIVE', 'DISABLED'));

create index question_status_idx on question (status);
