-- lukker feilet prosesstask
update prosess_task set status='KJOERT' where id = 15065704 and status='FEILET';

-- Rull tilbake Omsorgspenge-behandling som mangler aktivitetstatus etter kjørt beregning
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id from fagsak f
        inner join behandling b on f.id = b.fagsak_id
where f.ytelse_type = 'OMP' and f.saksnummer = '72ZEC' and b.behandling_status = 'UTRED';
