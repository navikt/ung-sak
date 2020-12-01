delete from fagsak_prosess_task fpt
where exists (select 1 from prosess_task p where p.id=fpt.prosess_task_id and p.status IN ('KJOERT', 'FERDIG'));
