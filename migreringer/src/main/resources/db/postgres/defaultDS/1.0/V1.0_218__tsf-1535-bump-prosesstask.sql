--TSF-1535
--saksnummer 7GB3S

delete from fagsak_prosess_task where prosess_task_id=14658382;
update prosess_task set status='KJOERT' where id = 14658382;
update prosess_task set status='KLAR', blokkert_av=null where blokkert_av=14658382 and status not in ('KLAR', 'KJOERT', 'FERDIG');
