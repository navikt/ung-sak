insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id from fagsak f
        inner join behandling b on f.id = b.fagsak_id
where f.ytelse_type = 'OMP' and f.saksnummer = '911Uo' and b.behandling_status = 'UTRED';
