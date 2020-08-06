-- start behandling på nytt
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7BB0G' and b.id=1048601;
            
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='82U96' and b.id=1084624;
            