UPDATE PROSESS_TASK SET neste_kjoering_etter = neste_kjoering_etter - INTERVAL '60 minutes' WHERE task_type = 'forvaltning.opprettManuellRevurdering' AND status = 'KLAR' AND neste_kjoering_etter IS NOT NULL;