update mottatt_dokument set status='UGYLDIG', behandling_id=null where journalpost_id='477949703';


-- henlegg behandling 1161901 på 8oAiK
with saker as (select b.fagsak_id, f.saksnummer, b.id as behandling_id 
 from fagsak f inner join behandling b on b.fagsak_id=f.id 
 where b.behandling_status NOT IN ('AVSLU', 'IVED') and f.saksnummer IN ('8oAH6')) 
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere) 
select nextval('seq_prosess_task'), 'behandlingskontroll.henleggBehandling', nextval('seq_prosess_task_gruppe'), null, 
'callId=CallId_FAGSYSTEM-120565-122668-' || saksnummer || '
fagsakId=' || saker.fagsak_id || '
behandlingId=' || saker.behandling_id || '
henleggesGrunn=HENLAGT_FEILOPPRETTET'
from saker;

