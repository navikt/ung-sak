WITH aksjonspunkter as (
    SELECT string_agg(aksjonspunkt_def, ',' ORDER BY 1) as aksjonspunkt_def, b.fagsak_id, a.behandling_id
    FROM AKSJONSPUNKT a 
    INNER JOIN BEHANDLING b ON (b.id = a.behandling_id) 
    INNER JOIN Fagsak f ON (f.id = b.fagsak_id)
    WHERE a.aksjonspunkt_def in ('5028', '5056')
        AND a.aksjonspunkt_status = 'OPPR'
        AND f.ytelse_type = 'OMP'
    GROUP BY fagsak_id, behandling_id
    ORDER BY fagsak_id, behandling_id
)
INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'),
  'behandlingskontroll.fortsettBehandling',
  nextval('seq_prosess_task_gruppe'),
  current_timestamp at time zone 'UTC' + (row_number() OVER()) * INTERVAL '1 seconds',
  'fagsakId=' || fagsak_id || '
behandlingId=' || behandling_id || '
aksjonspunktStatusTilUtfort='|| aksjonspunkt_def
FROM aksjonspunkter
;