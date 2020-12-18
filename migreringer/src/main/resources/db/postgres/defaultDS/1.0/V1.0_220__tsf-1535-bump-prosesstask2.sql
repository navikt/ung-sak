--TSF-1535
--saksnummer 7GB3S
delete from fagsak_prosess_task where prosess_task_id=14658430;
update prosess_task set status='KJOERT' where id = 14658430;
update prosess_task set status='KLAR', blokkert_av=null where blokkert_av=14658430 and status not in ('KLAR', 'KJOERT', 'FERDIG');


--saksnummer 9UAQU
-- resetter begge vurder kompletthet og lar sak kjøre på nytt
delete from fagsak_prosess_task where prosess_task_id=14658378;
update prosess_task set status='KJOERT' where id = 14658378;
update prosess_task set status='KLAR', blokkert_av=null where blokkert_av=14658378 and status not in ('KLAR', 'KJOERT', 'FERDIG');

update mottatt_dokument set status='BEHANDLER' where behandling_id=1303337;
