insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer in (
                        '6EML2',
                        '5ZCGW'
                    )
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'FASTSETT_STP_BER'
                    and bst.aktiv = true) > 0;
