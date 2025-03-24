insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
select nextval('SEQ_PROSESS_TASK'), 'batch.automatiskGjenopptaglese', 1, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1,'05'
where not exists (select 1 from prosess_task where task_type = 'batch.automatiskGjenopptaglese'  and status IN ('KLAR', 'FEILET'));

insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
select nextval('SEQ_PROSESS_TASK'), 'batch.opprettRevurderingHøySats', 1, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1,'05'
where not exists (select 1 from prosess_task where task_type = 'batch.opprettRevurderingHøySats'  and status IN ('KLAR', 'FEILET'));

insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
select nextval('SEQ_PROSESS_TASK'), 'daglig.sensu.metrikk.task', 1, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1,'05'
where not exists (select 1 from prosess_task where task_type = 'daglig.sensu.metrikk.task'  and status IN ('KLAR', 'FEILET'));

insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
select nextval('SEQ_PROSESS_TASK'), 'batch.gjenopptaVenterPåTilbakekreving', 1, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1,'05'
where not exists (select 1 from prosess_task where task_type = 'batch.gjenopptaVenterPåTilbakekreving'  and status IN ('KLAR', 'FEILET'));

insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
select nextval('SEQ_PROSESS_TASK'), 'batch.partitionCleanBucket', 1, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1,'05'
where not exists (select 1 from prosess_task where task_type = 'batch.partitionCleanBucket'  and status IN ('KLAR', 'FEILET'));

insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
select nextval('SEQ_PROSESS_TASK'), 'batch.retryFeilendeTasks', 1, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1,'05'
where not exists (select 1 from prosess_task where task_type = 'batch.retryFeilendeTasks'  and status IN ('KLAR', 'FEILET'));

insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
select nextval('SEQ_PROSESS_TASK'), 'batch.opprettRevurderingForInntektskontrollBatch', 1, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1,'05'
where not exists (select 1 from prosess_task where task_type = 'batch.opprettRevurderingForInntektskontrollBatch'  and status IN ('KLAR', 'FEILET'));
