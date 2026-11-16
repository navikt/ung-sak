insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens)
values (nextval('seq_prosess_task'), 'batch.migrerIkkeRelevantAktivitetsvilkaar', 0, 'KLAR', nextval('seq_prosess_task_gruppe')::text, '1');
