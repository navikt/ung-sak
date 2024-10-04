-- TSFF-874
-- Etter kjørt g-regulering 2024-05-28 blei det oppdaget saker som burde ha vore g-regulert, men som ikkje blei det.
-- Dette er fordi dei hadde skjæringstidspunkt fram i tid og utenfor intervallet som blei oppgitt som søkeparameter for G-reguleringa (2024-05-01/2024-05-28)
-- Saker for juni blei kjørt i ein eigen task. Her kjører kandidatutprøving på dei resterende som hadde behandling som passerte beregning før ny G-verdi blei lagt inn i løsninga (2024-05-26)
-- og med skjæringstidspunkt etter 2024-07-01
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'),
       'gregulering.kandidatUtprøving',
       nextval('seq_prosess_task_gruppe'),
       null,
       'fom="2024-07-01"
                        tom="2024-11-30"
                        fagsakId=' || fagsaker_med_behov_for_gregulering.id || ''

from (select distinct b.fagsak_id id from gr_beregningsgrunnlag gr
                                              inner join public.behandling b on b.id = gr.behandling_id
                                              inner join bg_perioder perioder on gr.bg_grunnlag_id = perioder.id
                                              inner join bg_periode periode on perioder.id = periode.bg_grunnlag_id
                                              inner join fagsak f on f.id = b.fagsak_id
      where periode.skjaeringstidspunkt >= '2024-07-01' and periode.opprettet_tid <= '2024-05-26' and b.avsluttet_dato is not null and aktiv = true and f.ytelse_type != 'OBSOLETE') fagsaker_med_behov_for_gregulering;
