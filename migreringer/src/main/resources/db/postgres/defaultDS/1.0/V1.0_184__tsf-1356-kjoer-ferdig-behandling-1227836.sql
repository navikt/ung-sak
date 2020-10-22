INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'),
  'behandlingskontroll.fortsettBehandling',
  nextval('seq_prosess_task_gruppe'),
  null,
  'fagsakId=' || b.fagsak_id || '
    behandlingId=' || b.id
from behandling b where 
 b.id = 1227836 and b.fagsak_id=1086774 and b.behandling_status='IVED';
