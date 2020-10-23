-- setter inn en ny for https://jira.adeo.no/browse/TSF-1331
insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key, task_parametere)
SELECT nextval('SEQ_PROSESS_TASK'), 'forvaltning.håndterFortaptDokument', 1, 'KLAR',
       nextval('SEQ_PROSESS_TASK_GRUPPE'), 1, '08', pt.task_parametere
from prosess_task pt where pt.id = 12204383 and pt.status in ('FEILET', 'KLAR');
