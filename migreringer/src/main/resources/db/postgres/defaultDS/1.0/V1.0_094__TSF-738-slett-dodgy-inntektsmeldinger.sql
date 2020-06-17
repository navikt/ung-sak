-- TSF-738 saksnummer 77F5Q, 7iTYM, 7HBSM (OMSORGSPENGER)
update prosess_task set status='KJOERT', blokkert_av=NULL where id in(
    1854417, 1780939, 1702166) and status NOT IN ('KJOERT', 'FERDIG');

delete from fagsak_prosess_task where prosess_task_id in (
    1854417, 1780939, 1702166) 
     and fagsak_id IN (1044177, 1053394, 1054876);

update prosess_task set status='KLAR', blokkert_av=NULL where blokkert_av in(
    1854417, 1780939, 1702166) 
     and status NOT IN ('KJOERT', 'FERDIG');
 
-- fjerner ugyldig inntektsmeldinger fra mottatt_dokument
delete from mottatt_dokument where fagsak_id in (1044177, 1053394, 1054876) and behandling_id is null;