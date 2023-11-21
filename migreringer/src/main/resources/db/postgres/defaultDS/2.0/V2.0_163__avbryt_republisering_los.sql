UPDATE prosess_task SET status = 'KJOERT'
    WHERE task_type = 'oppgavebehandling.RepubliserEvent'
    AND status = 'KLAR';
