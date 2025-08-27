insert into prosess_task (id, task_type)
select nextval('SEQ_PROSESS_TASK'), 'bigquery.metrikk.daglig.task'
where not exists (select 1 from prosess_task where task_type = 'bigquery.metrikk.daglig.task'  and status IN ('KLAR', 'FEILET'));
