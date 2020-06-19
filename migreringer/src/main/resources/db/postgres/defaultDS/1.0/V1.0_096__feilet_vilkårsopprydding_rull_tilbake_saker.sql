insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'beregning.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id from fagsak f
            inner join behandling b on f.id = b.fagsak_id
            inner join fagsak_prosess_task fpt on fpt.fagsak_id = f.id
            inner join prosess_task pt on pt.id = fpt.prosess_task_id
            where pt.task_type ='behandlingskontroll.fortsettBehandling'
            and pt.status = 'FEILET'
            and fpt.behandling_id = '' + b.id
            and pt.siste_kjoering_feil_tekst like '%FT-KALKULUS-INPUT-1000000: Kalkulus finner ikke kalkulator input for koblingId%'
            and f.ytelse_type = 'FRISINN';
