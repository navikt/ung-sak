insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
  select nextval('seq_prosess_task'),
  'iverksetteVedtak.publiserStonadstatistikk',
  nextval('seq_prosess_task_gruppe'),
  current_timestamp at time zone 'UTC' + interval '5 minutes',
  'saksnummer=' || f.saksnummer || '
behandlingId=' || b.id
FROM Behandling b INNER JOIN Fagsak f ON (
  f.id = b.fagsak_id
)
WHERE b.behandling_status IN ('AVSLU', 'IVED')
  AND f.ytelse_type = 'PSB'
  AND f.saksnummer IN ('AGSXi', 'AVWES', 'AMF7G')
ORDER BY b.opprettet_dato ASC;
