UPDATE prosess_task SET status = 'KJOERT', task_payload='STOPPET_MANUELT'
    WHERE task_type = 'oppgavebehandling.RepubliserEvent'
    AND status = 'KLAR';
