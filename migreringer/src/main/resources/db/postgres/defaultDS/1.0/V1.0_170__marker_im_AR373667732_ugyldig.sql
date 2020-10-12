update mottatt_dokument set status='UGYLDIG' where kanalreferanse='AR373667732' and journalpost_id='476403124';

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7FE5o' and b.id=1052260 and b.behandling_status='UTRED';
            

