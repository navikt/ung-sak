insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '7C1MS'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '6VBS4'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '77MRC'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '73A78'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '6R8WM'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '7970C'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '6PYKK'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '76Z9i'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '6Q2GU'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '7L4SK'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '7KFK8'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '7AVT8'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '7AVJi'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
                inner join behandling b on f.id = b.fagsak_id
                where f.saksnummer = '7CD0i'
                and b.behandling_status = 'UTRED'
                and (select count(*) from behandling_steg_tilstand bst
                    where bst.behandling_id = b.id
                    and bst.behandling_steg = 'KOFAKBER'
                    and bst.aktiv = true) > 0;
