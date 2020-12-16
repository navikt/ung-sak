update mottatt_dokument set 
  status='UGYLDIG', 
  feilmelding='Skal utbetales til bruker'
where journalpost_id IN  ('490160086','490007680');

delete from fagsak_prosess_task where prosess_task_id=14589409; 
update prosess_task set status='KJOERT' where id=14589409;  -- resetter lagring abakus for ovennevnte, forsøker deretter å la prosess gå videre

-- bouncer alle feilde tasks med en gang.
update prosess_task set 
   status='KLAR'
 , feilede_forsoek=0
 , neste_kjoering_etter=current_timestamp at time zone 'UTC' + interval '5 minutes'
 where status='FEILET' and neste_kjoering_etter is null;
