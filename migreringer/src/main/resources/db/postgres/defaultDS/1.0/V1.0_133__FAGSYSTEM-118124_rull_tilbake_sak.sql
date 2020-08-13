insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer in ('61620')
                and b.behandling_status = 'UTRED';
