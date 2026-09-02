update question q
set question_type = case
    when (select count(*) from question_answer a where a.question_id = q.id) = 1 then 'SINGLE_CHOICE'
    else 'MULTIPLE_CHOICE'
end;
