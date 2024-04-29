UPDATE prosess_task
SET neste_kjoering_etter = current_timestamp at time zone 'UTC' + floor(25200 + random() * 2 * 3600 * 24) * '1 second'::interval
WHERE task_type = 'oppgavebehandling.RepubliserEvent'
  AND status = 'KLAR';