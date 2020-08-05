INSERT INTO PROSESS_TASK_TYPE (
KODE,
NAVN,
FEIL_MAKS_FORSOEK,
FEIL_SEK_MELLOM_FORSOEK,
FEILHANDTERING_ALGORITME,
BESKRIVELSE)
VALUES (
'behandlingskontroll.tilbakeTilStart',
'Flytt tilbake til start',
1,
30,
'DEFAULT',
'Flytter til start av prosess');


-- unlink 2019 inntektsmeldinger

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='7BB0G') and journalpost_id IN ('474388528', '475736497');

update mottatt_dokument 
 set status='UGYLDIG', feilmelding='Inntektsmelding gjelder 2019. Kan ikke kobles til 2020 fagsak for omsorgspenger', behandling_id=null
 where fagsak_id in (select id from fagsak f where f.saksnummer='82U96') and journalpost_id IN ('479767704');

-- start behandling på nytt
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='7BB0G' and b.id=1048601;
            
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id 
                from behandling b
                inner join fagsak f on f.id=b.fagsak_id
                where f.saksnummer='82U96' and b.id=1084624;
            
            

