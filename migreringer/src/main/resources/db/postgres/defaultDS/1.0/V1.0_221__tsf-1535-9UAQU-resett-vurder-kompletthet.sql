
--saksnummer 9UAQU
-- resetter begge vurder kompletthet og lar sak kjøre på nytt
delete from fagsak_prosess_task where prosess_task_id=14658381;
update prosess_task set status='KJOERT' where id = 14658381;
update prosess_task set status='KLAR', blokkert_av=null where blokkert_av=14658381 and status not in ('KLAR', 'KJOERT', 'FERDIG');
