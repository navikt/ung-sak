INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'),
       'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'),
       current_timestamp at time zone 'UTC' + interval '15 minutes',
       'fagsakId=' || f.id || '
  behandlingId=' || b.id || '
  startSteg=VURDER_TILBAKETREKK'
FROM AKSJONSPUNKT a INNER JOIN BEHANDLING b ON (
        b.id = a.behandling_id
    ) INNER JOIN Fagsak f ON (
        f.id = b.fagsak_id
    )
WHERE a.aksjonspunkt_def IN ('5090')
  AND a.aksjonspunkt_status = 'OPPR'
  AND b.behandling_status IN ('OPPRE', 'UTRED')
  AND f.ytelse_type = 'OMP';
