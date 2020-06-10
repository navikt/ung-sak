insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id from behandling b
            inner join aksjonspunkt a on b.id = a.behandling_id
            where a.aksjonspunkt_def = '5038'
            and a.aksjonspunkt_status = 'OPPR'
            and a.opprettet_tid < '2020-06-05 07:30:00.000';
commit;
