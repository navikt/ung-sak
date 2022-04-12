INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'),
       'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'),
       null,
       'fagsakId=' || f.id || '
  behandlingId=' || b.id || '
  startSteg=FORVEDSTEG'
FROM AKSJONSPUNKT a INNER JOIN BEHANDLING b ON (
        b.id = a.behandling_id
    ) INNER JOIN Fagsak f ON (
        f.id = b.fagsak_id
    )
WHERE a.aksjonspunkt_def IN ('5040')
  AND a.aksjonspunkt_status = 'OPPR'
  AND (a.endret_tid is null OR a.endret_tid < '2022-02-02')
  AND a.opprettet_tid < '2022-01-01'
LIMIT 100;
