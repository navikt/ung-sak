INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
VALUES (nextval('seq_prosess_task'), 'omp.oppfriskalle', nextval('seq_prosess_task_gruppe'), current_date + time '23:30', null);
