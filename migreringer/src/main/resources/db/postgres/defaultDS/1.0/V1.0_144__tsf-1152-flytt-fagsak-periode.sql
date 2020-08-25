update fagsak set periode='[2020-01-01, 2021-01-01)'
 where saksnummer IN ('7TSEM', '7Y29i', '8o3GE');

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer IN ('7TSEM', '7Y29i', '8o3GE') and b.id IN (1160904, 1160902, 1160888)
                and b.behandling_status='UTRED' and b.behandling_resultat_type='IKKE_FASTSATT' 
;



update mottatt_dokument set fagsak_id=1050268, behandling_id=null 
 where journalpost_id='481718498';


-- henlegg behandling 1161901 på 8oAiK
with saker as (select b.fagsak_id, f.saksnummer, b.id as behandling_id 
 from fagsak f inner join behandling b on b.fagsak_id=f.id 
 where b.behandling_status NOT IN ('AVSLU', 'IVED') and f.saksnummer IN ('8oAiK')) 
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere) 
select nextval('seq_prosess_task'), 'behandlingskontroll.henleggBehandling', nextval('seq_prosess_task_gruppe'), null, 
'callId=CallId_FAGSYSTEM-120565-122668-' || saksnummer || '
fagsakId=' || saker.fagsak_id || '
behandlingId=' || saker.behandling_id || '
henleggesGrunn=HENLAGT_FEILOPPRETTET'
from saker;


-- opprett revurdering 7DXDo med ny inntektsmelding overført fra 8oAiK

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere) 
 select nextval('seq_prosess_task'), 'innhentsaksopplysninger.håndterMottattDokument', 
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes', 
'fagsakId=' || m.fagsak_id || '
arsakType=RE-ANNET
mottattDokumentId=' || m.id
                from mottatt_dokument m
                
                where status='GYLDIG' AND type='INNTEKTSMELDING' AND 
                journalpost_id in ('481718498');

