update prosess_task set neste_kjoering_etter=current_timestamp at time zone 'UTC' + interval '5 minutes'
where status ='KLAR' and task_type='batch.retryFeilendeTasks';
