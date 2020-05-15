UPDATE aksjonspunkt set frist_tid = current_date - interval '1 day' WHERE aksjonspunkt_def = '8000' AND aksjonspunkt_status = 'OPPR';
UPDATE prosess_task set neste_kjoering_etter = neste_kjoering_etter - interval '1 day' WHERE task_type = 'batch.automatiskGjenopptaglese' AND status = 'KLAR';
