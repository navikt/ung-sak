INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'), 'oppgavebehandling.RepubliserEvent',
       nextval('seq_prosess_task_gruppe'), null,
       'behandlingUuid=' || b.uuid
FROM Behandling b INNER JOIN Fagsak f ON (
    f.id = b.fagsak_id
  )
WHERE b.behandling_status IN ('OPPRE', 'UTRED')
  AND f.ytelse_type IN ('PSB', 'PPN');