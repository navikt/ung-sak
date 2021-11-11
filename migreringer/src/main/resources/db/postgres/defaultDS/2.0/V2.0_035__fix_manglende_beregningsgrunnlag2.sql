insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'),
  'fix.psbFeilrette',
  nextval('seq_prosess_task_gruppe'),
  current_timestamp at time zone 'UTC' + interval '5 minutes',
  'saksnummer=' || f.saksnummer
FROM Fagsak f
WHERE f.ytelse_type = 'PSB';