

update mottatt_dokument set status = 'GYLDIG', behandling_id=null, feilmelding='Refusjon periode går over flere år' 
where journalpost_id in (
'476031506',
'477702713',
'477694925',
'476906647',
'474910964',
'477949703',
'478167962',
'478607079',
'477584757',
'478605955',
'479669531',
'479767006',
'480317188'
);
--(lar mottatt_dokument.feilmelding stå for reference)


insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
 select nextval('seq_prosess_task'), 'innhentsaksopplysninger.håndterMottattDokument',
       nextval('seq_prosess_task_gruppe'), current_timestamp at time zone 'UTC' + interval '3 minutes',
'fagsakId=' || m.fagsak_id || '
arsakType=RE-ANNET
mottattDokumentId=' || m.id
                from mottatt_dokument m
                
                where status='GYLDIG' AND type='INNTEKTSMELDING' AND 
                journalpost_id in (
'476031506',
'477702713',
'477694925',
'476906647',
'474910964',
'477949703',
'478167962',
'478607079',
'477584757',
'478605955',
'479669531',
'479767006',
'480317188'
                )
                 ;
