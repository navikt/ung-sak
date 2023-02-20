insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id  || '
                startSteg=KOFAKUT' from behandling b
                                                                                     inner join aksjonspunkt a on b.id = a.behandling_id
where a.aksjonspunkt_def = '9203'
  and a.aksjonspunkt_status = 'OPPR'
  and a.opprettet_tid > '2022-09-06 07:47:00.000'
  and a.opprettet_tid < '2022-09-07 07:33:00.000'
  and (a.endret_tid is null OR a.endret_tid < '2022-09-07 07:33:00.000');
