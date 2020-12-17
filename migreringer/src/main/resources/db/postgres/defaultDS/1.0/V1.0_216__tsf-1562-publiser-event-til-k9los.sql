
-- korrigerer 7VPQG for å linke riktig ifht ha k9-oppdrag kjenner til
update behandling set original_behandling_id=1076757       
where id = 1308990   and behandling_status='UTRED';

-- korrigerer 732Zi for å linke riktig ifht hva k9-oppdrag kjenner til
update behandling set original_behandling_id=1092375       
where id = 1308944     and behandling_status='UTRED';


-- publiser manglende event til los for avsluttede behandlinger (som ikke har fått avsluttet dato)
with avsluttet as (
select '{
  "eksternId" : "' || b.uuid || '",
  "fagsystem" : {
    "kode" : "K9SAK",
    "kodeverk" : "FAGSYSTEM"
  },
  "behandlingstidFrist" : "' || replace(b.behandlingstid_frist::text, ' ' ,'T') || '",
  "saksnummer" : "' || f.saksnummer || '",
  "aktørId" : "' || f.bruker_aktoer_id || '",
  "behandlingId" : ' || b.id || ',
  "eventTid" : "' || replace((current_timestamp at time zone 'UTC')::text, ' ', 'T') || '",
  "eventHendelse" : "AKSJONSPUNKT_AVBRUTT",
  "behandlinStatus" : "AVSLU",
  "behandlingStatus" : "AVSLU",
  "behandlingSteg" : null,
  "behandlendeEnhet" : null,
  "ansvarligBeslutterForTotrinn" : null,
  "ansvarligSaksbehandlerForTotrinn" : null,
  "ytelseTypeKode" : "OMP",
  "behandlingTypeKode" : "' || b.behandling_type || '",
  "opprettetBehandling" : "' || replace(b.opprettet_tid::text, ' ', 'T') || '",
  "aksjonspunktKoderMedStatusListe" : { }
}' as payload,
b.id as behandling_id,
f.id as fagsak_id,
f.saksnummer
 from behandling b inner join fagsak f on f.id=b.fagsak_id
 where b.behandling_status='AVSLU' and b.avsluttet_dato is null
)
INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_payload, task_parametere)
SELECT nextval('seq_prosess_task'),
  'oppgavebehandling.PubliserEvent',
  nextval('seq_prosess_task_gruppe'),
  current_timestamp at time zone 'UTC' + interval '2 minutes',
  payload,
  'fagsakId=' || fagsak_id || '
behandlingId=' || behandling_id || '
topicKey='|| behandling_id
FROM avsluttet
;


-- set avsluttet dato for avsluttede behandlinger
update behandling set avsluttet_dato = current_timestamp at time zone 'UTC' - interval '6 hours'  where behandling_status ='AVSLU' and avsluttet_dato is null;

