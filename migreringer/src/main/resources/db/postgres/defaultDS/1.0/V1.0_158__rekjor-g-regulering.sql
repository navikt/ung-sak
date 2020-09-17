INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_payload)
SELECT nextval('seq_prosess_task'),
  'forvaltning.opprettManuellRevurdering',
  nextval('seq_prosess_task_gruppe'),
  (TIMESTAMP '2020-09-18 05:40:00' at time zone 'UTC') + (row_number() OVER()) * INTERVAL '1 seconds',
  f.saksnummer
FROM (
  SELECT DISTINCT b.fagsak_id
  FROM VR_VILKAR_PERIODE p INNER JOIN VR_VILKAR v ON (
      v.id = p.vilkar_id
  ) INNER JOIN VR_VILKAR_RESULTAT r ON (
      r.id = v.vilkar_resultat_id
  ) INNER JOIN RS_VILKARS_RESULTAT r2 ON (
      r2.vilkarene_id = r.id
      AND r2.aktiv = true
  ) INNER JOIN Behandling b ON (
      b.id = r2.behandling_id
  )
  WHERE p.fom >= to_date('2020-05-01', 'YYYY-MM-DD')
    AND v.vilkar_type = 'FP_VK_41'
    AND NOT EXISTS (
      SELECT *
      FROM Behandling b2
      WHERE b2.id != b.id
        AND b2.opprettet_dato > b.opprettet_dato
        AND b2.fagsak_id = b.fagsak_id
    )
) b INNER JOIN Fagsak f ON (
  f.id = b.fagsak_id
  AND f.ytelse_type = 'OMP'
)
;