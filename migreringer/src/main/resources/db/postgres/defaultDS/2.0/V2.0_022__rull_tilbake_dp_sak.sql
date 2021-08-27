-- Ruller tilbake sak som har blitt rammet av feil i fordeling
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'),
       'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'),
       null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id
from fagsak f
         inner join behandling b on f.id = b.fagsak_id
where f.saksnummer = 'ABDMY'
  and b.behandling_status = 'UTRED'
  and (select count(*)
       from behandling_steg_tilstand bst
       where bst.behandling_id = b.id
         and bst.behandling_steg = 'FAST_BERGRUNN'
         and bst.aktiv = true) > 0;
