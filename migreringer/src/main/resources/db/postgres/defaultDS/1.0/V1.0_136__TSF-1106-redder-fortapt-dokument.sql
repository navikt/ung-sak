-- setter inn en ny
insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key, task_parametere)
SELECT nextval('SEQ_PROSESS_TASK'), 'forvaltning.håndterFortaptDokument', 1, 'KLAR',
       nextval('SEQ_PROSESS_TASK_GRUPPE'), 1, '08', pt.task_parametere
from prosess_task pt where pt.id = 1558450 and pt.task_type = 'innhentsaksopplysninger.håndterMottattDokument';
