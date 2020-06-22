-- Duplikatbehandling som har stoppet opp. Feilende task 'iverksetteVedtak.oppdragTilØkonomi' er allerede fjernet.
-- Saken henlegges gjennom databaseskript
-- Saksnummer 65S3S FRISINN

-- Forholdsregel: Rydde opp tasker
update prosess_task set status='KJOERT', blokkert_av=NULL
where id IN (select prosess_task_id from fagsak_prosess_task where behandling_id = '1007216')
  and status NOT IN ('KJOERT', 'FERDIG');
delete from fagsak_prosess_task where behandling_id = '1007216';
-- Forholdsregel: Rydde opp aksjonspunkt
update aksjonspunkt set aksjonspunkt_status='AVBR' where aksjonspunkt_status='OPPR' AND behandling_id = 1007216;

-- Henlegg og avslutt behandling
update behandling
set behandling_resultat_type = 'HENLAGT_FEILOPPRETTET'
  , behandling_status = 'AVSLU'
  , avsluttet_dato = now()
where id = 1007216
and avsluttet_dato is null;

-- Oppdater fagsak (har også innvilget behandling)
update fagsak
set fagsak_status = 'LOP'
where saksnummer = '65S3S'
;
