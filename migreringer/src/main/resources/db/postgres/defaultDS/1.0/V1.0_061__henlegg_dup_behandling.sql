

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere) 
select nextval('seq_prosess_task'), 'behandlingskontroll.henleggBehandling', nextval('seq_prosess_task_gruppe'), null,
'callId=CallId_TSF-556-60bf2e56-780d-4cf3-b383-ed2d7741a040
fagsakId=1007324
behandlingId=1007216
henleggesGrunn=HENLAGT_FEILOPPRETTET'
from behandling b
inner join fagsak f on f.id = b.fagsak_id
where f.id=1007324 and b.id=1007216;
