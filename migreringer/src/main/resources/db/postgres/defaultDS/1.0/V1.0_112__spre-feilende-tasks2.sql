UPDATE prosess_task
SET neste_kjoering_etter = current_timestamp at time zone 'UTC' + floor(random() * 1800) * '1 second'::interval,
    feilede_forsoek=0,
    status='KLAR'
WHERE task_type = 'innhentsaksopplysninger.håndterMottattDokument' AND status = 'FEILET';