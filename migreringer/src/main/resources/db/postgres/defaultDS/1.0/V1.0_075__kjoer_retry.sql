		   
update prosess_task set neste_kjoering_etter = current_timestamp at time zone 'UTC' + interval '5 minutes' where task_type='batch.retryFeilendeTasks' and status='KLAR';