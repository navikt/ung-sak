-- Rull tilbake Omsorgspenge-behandlinger med åpent aksjonspunkt 5058
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id from fagsak f
        inner join behandling b on f.id = b.fagsak_id
        inner join aksjonspunkt a on b.id = a.behandling_id
where a.aksjonspunkt_def = '5058'
  and a.aksjonspunkt_status = 'OPPR'
  and f.ytelse_type = 'OMP';
