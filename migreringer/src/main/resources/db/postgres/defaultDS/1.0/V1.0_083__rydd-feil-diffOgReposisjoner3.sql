
update prosess_task set blokkert_av=null, status='KLAR' 
where status='VETO' AND blokkert_av in (select id from prosess_task where  task_type='behandlingskontroll.fortsettBehandling' and status='FERDIG' and blokkert_av IN (1876028, 1881428));

update prosess_task set blokkert_av=null 
where blokkert_av in (select id from prosess_task where  task_type='behandlingskontroll.fortsettBehandling' and status='FERDIG' and blokkert_av IN (1876028, 1881428));

update prosess_task set 
blokkert_av=null, 
status = 'KLAR'
where blokkert_av IN (1876028, 1881428) and status='VETO';

update prosess_task set blokkert_av=null where blokkert_av IN (1876028, 1881428);