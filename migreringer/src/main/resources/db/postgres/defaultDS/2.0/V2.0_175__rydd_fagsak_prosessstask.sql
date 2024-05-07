
delete from fagsak_prosess_task fpt
where fpt.prosess_task_id
    in (21459802,  3777133, 3773045, 3777402)
  and not exists(select 1 from prosess_task pt where pt.id = fpt.id);
