delete from prosess_task where task_type='sensu.metrikk.task' and status ='KLAR';

-- setter inn en ny
 insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
 values (nextval('SEQ_PROSESS_TASK'), 'sensu.metrikk.task', 1, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1, '05');
 