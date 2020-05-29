update aksjonspunkt set frist_tid = current_timestamp at time zone 'UTC' where aksjonspunkt_def='7003' and aksjonspunkt_status='OPPR';
		   
update prosess_task set neste_kjoering_etter = current_timestamp at time zone 'UTC' + interval '5 minutes' where task_type='batch.automatiskGjenopptaglese' and status='KLAR';