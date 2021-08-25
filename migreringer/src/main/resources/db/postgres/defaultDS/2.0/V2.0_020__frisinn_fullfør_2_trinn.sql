-- Frisinn har kommet i ugyldig tilstand, skal aldri havne i 2-trinnskontroll (5016)
-- Setter 5016 som utført og fortsetter behandling til iverksettelse
update aksjonspunkt
set aksjonspunkt_status='UTFO'
where aksjonspunkt_status='OPPR'
  and aksjonspunkt_def = '5016' AND behandling_id IN (1406907, 1400658, 1416359, 1415696, 1416013);


insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'behandlingskontroll.fortsettBehandling',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || f.id || '
                behandlingId=' || b.id from fagsak f
inner join behandling b on f.id = b.fagsak_id
where f.saksnummer IN ('ADU98', 'AD034', '9W0o0', 'A5oGU', 'A7QUM')
  and b.behandling_status = 'FVED'
  and exists (select 1 from behandling_steg_tilstand bst
              where bst.behandling_id = b.id
                and bst.behandling_steg = 'FVEDSTEG'
                and bst.aktiv = true);
