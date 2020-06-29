-- Rull tilbake Frisinn-behandlinger med åpent aksjonspunkt 8004, som har passert sitt vurderingspunkt
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id from fagsak f
        inner join behandling b on f.id = b.fagsak_id
        inner join behandling_steg_tilstand bst on b.id = bst.behandling_id
        inner join aksjonspunkt a on b.id = a.behandling_id
where a.aksjonspunkt_def = '8004'
  and a.aksjonspunkt_status = 'OPPR'
  and a.opprettet_tid < '2020-06-30 00:00:00.000'
  and f.ytelse_type = 'FRISINN'
  and bst.behandling_steg = 'FASTSETT_STP_BER'
  and bst.aktiv = true;

-- Rydd opp tasker for behandlinger med aksjonspunkt 8004 (feiler når saksbehandler åpner behandling)
update prosess_task set status='KJOERT', blokkert_av=NULL
where siste_kjoering_feil_tekst like '%Det er definert aksjonspunkt [[8004]] som ikke er håndtert av noe steg fra og med: FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING%'
  and status NOT IN ('KJOERT', 'FERDIG');
delete from fagsak_prosess_task where prosess_task_id in (select id from prosess_task where siste_kjoering_feil_tekst like '%Det er definert aksjonspunkt [[8004]] som ikke er håndtert av noe steg fra og med: FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING%');

