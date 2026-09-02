alter table question
    drop constraint question_question_bank_id_fkey;

alter table question_option
    drop constraint question_option_question_id_fkey;

alter table question_answer
    drop constraint question_answer_question_option_fkey;
