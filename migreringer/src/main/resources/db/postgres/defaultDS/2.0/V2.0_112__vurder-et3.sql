INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'), 'drift.vurderOmEtablertTilsynErEndret',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '10 minutes',
       'saksnummer=' || f.saksnummer
FROM Fagsak f
WHERE f.ytelse_type = 'PSB'
  AND NOT EXISTS (
    SELECT *
    FROM Behandling b
    WHERE b.fagsak_id = f.id
      AND b.avsluttet_dato > to_date('2022-11-01', 'YYYY-MM-DD')
  )
;