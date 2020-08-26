INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'),
  'behandlingskontroll.tilbakeTilStart',
  nextval('seq_prosess_task_gruppe'),
  null,
  'fagsakId=' || f.id || '
    behandlingId=' || b.id || '
    startSteg=VURDER_UTTAK'  
FROM AKSJONSPUNKT a INNER JOIN BEHANDLING b ON (
    b.id = a.behandling_id
  ) INNER JOIN Fagsak f ON (
    f.id = b.fagsak_id
  )
  WHERE a.aksjonspunkt_def = '9003'
    AND a.aksjonspunkt_status = 'OPPR'
    AND f.ytelse_type = 'OMP';
