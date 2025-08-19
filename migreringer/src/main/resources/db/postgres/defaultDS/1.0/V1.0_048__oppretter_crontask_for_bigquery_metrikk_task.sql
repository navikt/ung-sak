insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
select nextval('SEQ_PROSESS_TASK'), 'bigquery.metrikk.task', 1, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1,'05'
where not exists (select 1 from prosess_task where task_type = 'bigquery.metrikk.task'  and status IN ('KLAR', 'FEILET'));
