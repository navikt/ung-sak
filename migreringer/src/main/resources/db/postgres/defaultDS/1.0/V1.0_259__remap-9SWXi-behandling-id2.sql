-- fix up mottatt_dokument prosess_task for 9SWXi
update fagsak_prosess_task set behandling_id='1350797' where prosess_task_id=15637812;

update prosess_task set task_parametere = replace(task_parametere, '1291654', '1350797')
where id=15637812 and status in ('FEILET', 'KLAR', 'VETO');

