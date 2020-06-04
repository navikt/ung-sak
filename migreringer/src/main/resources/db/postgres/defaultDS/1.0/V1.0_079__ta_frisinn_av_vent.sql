UPDATE aksjonspunkt set frist_tid = current_date - interval '1 day' WHERE aksjonspunkt_def = '8003' AND aksjonspunkt_status = 'OPPR' AND vent_aarsak = 'FRISINN_VARIANT_SN_MED_FL_INNTEKT';
UPDATE prosess_task set neste_kjoering_etter = neste_kjoering_etter - interval '1 day' WHERE task_type = 'batch.automatiskGjenopptaglese' AND status = 'KLAR';
