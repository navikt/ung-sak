insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '71J8K'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'FORS_BERGRUNN'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '65RDE'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'FORS_BERGRUNN'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '70ZoE'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'FORS_BERGRUNN'
                    and bst.aktiv = true) > 0;


insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '71EJ4'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'FORS_BERGRUNN'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '70PZS'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'FORS_BERGRUNN'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '70oHM'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'FORS_BERGRUNN'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '70TB8'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'FORS_BERGRUNN'
                    and bst.aktiv = true) > 0;
