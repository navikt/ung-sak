-- TSF-802 saksnummer 6KSM4 (OMSORGSPENGER)
update prosess_task set status='KJOERT', blokkert_av=NULL where id in(
    2030642, 2030643, 2030644) and status NOT IN ('KJOERT', 'FERDIG');

delete from fagsak_prosess_task where prosess_task_id in (
    2030642, 2030643, 2030644) 
     and fagsak_id = 1022563;

update prosess_task set status='KLAR', blokkert_av=NULL where blokkert_av in(
    2030642, 2030643, 2030644) 
     and status NOT IN ('KJOERT', 'FERDIG');
 
 
