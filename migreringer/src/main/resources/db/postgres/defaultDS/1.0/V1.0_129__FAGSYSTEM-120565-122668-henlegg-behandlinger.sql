-- Saker som skal henlegges:
-- FAGSYSTEM-120565: 650K4
-- FAGSYSTEM-122668: 7JYPA


--stopp refresh registerdata (forholdsregel)
update behandling set SIST_OPPDATERT_TIDSPUNKT=current_timestamp at time zone 'UTC'
where fagsak_id IN (select f.id from fagsak f where f.saksnummer IN ('650K4', '7JYPA')) AND  behandling_status NOT IN ('AVSLU', 'IVED');

-- fjern gamle tasks
update prosess_task set status='KJOERT', blokkert_av=NULL where id in (
    select prosess_task_id from fagsak_prosess_task fpt inner join fagsak f on f.id=fpt.fagsak_id and f.saksnummer IN ('650K4', '7JYPA'))
    and status NOT IN ('KJOERT', 'FERDIG');

delete from fagsak_prosess_task 
where fagsak_id IN (
 select f.id from  fagsak f where f.saksnummer IN ('650K4', '7JYPA')
) 
AND cast(behandling_id as bigint) IN (
  select b.id from behandling b where b.fagsak_id IN (select f.id from fagsak f where f.saksnummer IN ('650K4', '7JYPA')) AND  b.behandling_status NOT IN ('AVSLU', 'IVED')
);

-- legg in henlegg behandling task
with saker as (select b.fagsak_id, f.saksnummer, b.id as behandling_id
 from fagsak f inner join behandling b on b.fagsak_id=f.id
 where b.behandling_status NOT IN ('AVSLU', 'IVED') and f.saksnummer IN ('650K4', '7JYPA'))
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere) 
select nextval('seq_prosess_task'), 'behandlingskontroll.henleggBehandling', nextval('seq_prosess_task_gruppe'), null,
'callId=CallId_FAGSYSTEM-120565-122668-' || saksnummer || '
fagsakId=' || saker.fagsak_id || '
behandlingId=' || saker.behandling_id || '
henleggesGrunn=HENLAGT_FEILOPPRETTET'
from saker;

