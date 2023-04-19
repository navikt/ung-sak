insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'migrer.unntaketablerttilsynperiode.vurdertav', nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '5 minutes', 'behandlingId=' || b.id
FROM behandling b
  where exists (select 1 from psb_unntak_etablert_tilsyn_periode where kilde_behandling_id = b.id and vurdert_av is null);
