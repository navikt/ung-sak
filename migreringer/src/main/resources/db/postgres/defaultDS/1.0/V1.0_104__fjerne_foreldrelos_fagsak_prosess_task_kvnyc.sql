-- TSFF-3297: Henleggelsen av behandling 3018856 slettet prosesstaskene 1776563 og 1776570,
-- men ikke koblingene i fagsak_prosess_task. Koblingen til 1776563 har lavest gruppe_sekvensnr og
-- blokkerer behandlingskontroll.opprettRevurderingEllerDiff (1798171) for inntektskontroll av september.
delete from fagsak_prosess_task fpt
where fpt.fagsak_id = 2010102
  and fpt.prosess_task_id in (1776563, 1776570)
  and not exists (select 1 from prosess_task pt where pt.id = fpt.prosess_task_id);
