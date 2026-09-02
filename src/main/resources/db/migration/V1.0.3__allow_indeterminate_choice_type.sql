alter table question
    add constraint question_type_valid
    check (question_type in ('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'INDETERMINATE_CHOICE'));
