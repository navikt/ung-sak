-- https://jira.adeo.no/browse/TSF-1443
-- https://jira.adeo.no/browse/TSF-1538

-- rykker følgende tilbake tilstart: 7HBEQ, 994o4

INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'),
       'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'),
       current_timestamp at time zone 'UTC' + interval '5 minutes',
       'fagsakId=' || f.id || '
  behandlingId=' || b.id || '
  startSteg=START'
FROM BEHANDLING b  INNER JOIN Fagsak f ON (
        f.id = b.fagsak_id
    )
WHERE f.saksnummer IN ('7HBEQ', '994o4')
  AND b.behandling_status='UTRED'
  AND f.ytelse_type = 'OMP';