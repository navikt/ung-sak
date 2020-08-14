UPDATE prosess_task
SET neste_kjoering_etter = current_timestamp at time zone 'UTC',
    feilede_forsoek=0,
    status='KLAR'
WHERE task_type = 'forvaltning.håndterFortaptDokument' AND status = 'FEILET';
