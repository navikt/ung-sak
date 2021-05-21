update prosess_task set status='KJOERT' where task_type='rapportering.identCache' and status='KLAR'; -- fjerner gammel

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
values (nextval('seq_prosess_task'), 'rapportering.identCache', nextval('seq_prosess_task_gruppe'), null, null);