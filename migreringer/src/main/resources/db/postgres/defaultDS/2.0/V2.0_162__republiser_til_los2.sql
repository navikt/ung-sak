INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'), 'oppgavebehandling.RepubliserEvent',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + floor(600 + random() * 2 * 3600 * 24) * '1 second'::interval,
       'behandlingUuid=' || b.uuid
FROM Behandling b INNER JOIN Fagsak f ON (
    f.id = b.fagsak_id
  )
WHERE f.ytelse_type <> 'OBSOLETE'
;