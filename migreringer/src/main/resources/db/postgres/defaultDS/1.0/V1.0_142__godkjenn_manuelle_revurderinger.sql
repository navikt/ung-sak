INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'),
  'behandlingskontroll.fortsettBehandling',
  nextval('seq_prosess_task_gruppe'),
  current_timestamp at time zone 'UTC' + interval '15 minutes',
  'fagsakId=' || f.id || '
    behandlingId=' || b.id || '
    aksjonspunktStatusTilUtfort='||a.aksjonspunkt_def  
FROM AKSJONSPUNKT a INNER JOIN BEHANDLING b ON (
    b.id = a.behandling_id
  ) INNER JOIN Fagsak f ON (
    f.id = b.fagsak_id
  )
  WHERE a.aksjonspunkt_def in ('5028', '5056')
    AND aksjonspunkt_status = 'OPPR';