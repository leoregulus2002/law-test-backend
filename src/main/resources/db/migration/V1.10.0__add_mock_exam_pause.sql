alter table mock_exam_attempt
    add column remaining_seconds integer;

alter table mock_exam_attempt
    drop constraint mock_exam_attempt_status_valid;

alter table mock_exam_attempt
    add constraint mock_exam_attempt_status_valid
        check (status in ('IN_PROGRESS', 'PAUSED', 'SUBMITTED', 'ABANDONED'));
