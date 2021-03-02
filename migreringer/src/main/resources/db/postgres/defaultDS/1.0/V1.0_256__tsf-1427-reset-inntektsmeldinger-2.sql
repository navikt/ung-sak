-- FAGSYSTEM-155932
-- Det gjelder 2 IM mottatt henholdsvis
-- 25.11.2020 kl 20.35.15 AR398964018 for 22.06.2020 og 23.06.2020
-- 25.11.2020 kl 20.35.18 AR398964019 for 12.06.2020
update mottatt_dokument set status='MOTTATT' where journalpost_id in ('490674017','490674020');

insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key, task_parametere)
SELECT nextval('SEQ_PROSESS_TASK'), 'forvaltning.håndterFortaptDokument', 1, 'KLAR',
       1142138, 1, '08', 'mottattDokumentId=1890270
fagsakId=1142138
saksnummer=9SXWi';
       
insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key, task_parametere)
SELECT nextval('SEQ_PROSESS_TASK'), 'forvaltning.håndterFortaptDokument', 1, 'KLAR',
       1142138, 2, '08', 'mottattDokumentId=1890271
fagsakId=1142138
saksnummer=9SXWi'
       ;
       
-- opprett manuell revurdering
INSERT INTO prosess_task (id, task_type, task_gruppe, task_sekvens, partition_key, neste_kjoering_etter, task_parametere)
SELECT nextval('seq_prosess_task'),
  'forvaltning.opprettManuellRevurdering',
  1142138, 3, '08',
  (current_timestamp at time zone 'UTC') + (row_number() OVER()) * INTERVAL '1 seconds',
  'fagsakId=' || t.id || '
  saksnummer=' || t.saksnummer
FROM (
  select distinct f.saksnummer, f.id from fagsak f
  where f.saksnummer IN ('9SXWi')
  AND f.ytelse_type = 'OMP'
) t
;


