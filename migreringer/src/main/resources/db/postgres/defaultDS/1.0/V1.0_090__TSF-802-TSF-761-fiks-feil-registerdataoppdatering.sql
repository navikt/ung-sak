--
-- TSF-802
-- 

-- TSF-802 saksnummer 634B6 FRISINN
update prosess_task set status='KJOERT', blokkert_av=NULL where id in(
	1900069,
	1900070,
	1900071,
	1900072,
	1900073,
	1900074,
	1900075) and status NOT IN ('KJOERT', 'FERDIG');

delete from fagsak_prosess_task where prosess_task_id in (
	1900069,
	1900070,
	1900071,
	1900072,
	1900073,
	1900074,
	1900075) and fagsak_id = 1004742;

update prosess_task set status='KLAR', blokkert_av=NULL where blokkert_av in(
	1900069,
	1900070,
	1900071,
	1900072,
	1900073,
	1900074,
	1900075) and status NOT IN ('KJOERT', 'FERDIG');


-- TSF-802 saksnummer 6KMR0 OMSORGSPENGER
update prosess_task set status='KJOERT', blokkert_av=NULL where id in(
	2014945,
	2014946,
	2014947,
	2014948,
	2014949,
	2014950) and status NOT IN ('KJOERT', 'FERDIG');

delete from fagsak_prosess_task where prosess_task_id in (
	2014945,
	2014946,
	2014947,
	2014948,
	2014949,
	2014950) and fagsak_id = 1022411;

update prosess_task set status='KLAR', blokkert_av=NULL where blokkert_av in(
	2014945,
	2014946,
	2014947,
	2014948,
	2014949,
	2014950) and status NOT IN ('KJOERT', 'FERDIG');


-- TSF-802 saksnummer 6KSM4 (OMSORGSPENGER)
update prosess_task set status='KJOERT', blokkert_av=NULL where id in(
	2030641) and status NOT IN ('KJOERT', 'FERDIG');

delete from fagsak_prosess_task where prosess_task_id in (
	2030641) 
	 and fagsak_id = 1022563;

update prosess_task set status='KLAR', blokkert_av=NULL where blokkert_av in(
	2030641) 
	 and status NOT IN ('KJOERT', 'FERDIG');
 
 
--
-- TSF-761
-- 

-- TSF-761 saksnummer 6i8R6 OMSORGSPENGER
update prosess_task set status='KJOERT', blokkert_av=NULL where id in(
	1899555, 1899556) and status NOT IN ('KJOERT', 'FERDIG');

delete from fagsak_prosess_task where prosess_task_id in (
	1899555, 1899556) 
	 and fagsak_id = 1020183;

update prosess_task set status='KLAR', blokkert_av=NULL where blokkert_av in(
1899555, 1899556) 
 and status NOT IN ('KJOERT', 'FERDIG');
 
 
 -- TSF-761 saksnummer 6iVH8 OMSORGSPENGER
update prosess_task set status='KJOERT', blokkert_av=NULL where id in(
	1899393) and status NOT IN ('KJOERT', 'FERDIG');
	
delete from fagsak_prosess_task where prosess_task_id in (
	1899393) 
	 and fagsak_id = 1020771;

update prosess_task set status='KLAR', blokkert_av=NULL where blokkert_av in(
1899393) 
 and status NOT IN ('KJOERT', 'FERDIG');
 
 
-- TSF-761 saksnummer 6iNAS OMSORGSPENGER
update prosess_task set status='KJOERT', blokkert_av=NULL where id in(
	1899490) and status NOT IN ('KJOERT', 'FERDIG');

delete from fagsak_prosess_task where prosess_task_id in (
	1899490) 
	 and fagsak_id = 1020559;

update prosess_task set status='KLAR', blokkert_av=NULL where blokkert_av in(
	1899490) 
	 and status NOT IN ('KJOERT', 'FERDIG');
 
