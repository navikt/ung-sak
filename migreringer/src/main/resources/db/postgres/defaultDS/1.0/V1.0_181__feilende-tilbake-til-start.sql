INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'),
  'behandlingskontroll.tilbakeTilStart',
  nextval('seq_prosess_task_gruppe'),
  null,
  'fagsakId=' || f.id || '
    behandlingId=' || b.id  
FROM prosess_task p INNER JOIN BEHANDLING b ON (
    b.id = CAST(SUBSTRING(p.task_parametere, 'behandlingId=([^[:space:]]*)') AS BIGINT)
    AND b.id IS NOT NULL
  ) INNER JOIN Fagsak f ON (
    f.id = b.fagsak_id
  )
  WHERE p.status = 'FEILET'
    AND p.task_type = 'behandlingskontroll.fortsettBehandling'
    AND f.ytelse_type = 'OMP';