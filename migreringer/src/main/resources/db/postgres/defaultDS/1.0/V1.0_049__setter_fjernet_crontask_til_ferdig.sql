update prosess_task
set status = 'FERDIG'
where (task_type = 'sensu.metrikk.task' OR task_type = 'daglig.sensu.metrikk.task') and status IN ('KLAR', 'FEILET');
