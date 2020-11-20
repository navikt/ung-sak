delete from prosess_task where task_type='abakus.async.lagre';
delete from prosess_task_type where kode='abakus.async.lagre';

insert into PROSESS_TASK_TYPE (KODE,NAVN,FEIL_MAKS_FORSOEK,FEIL_SEK_MELLOM_FORSOEK,FEILHANDTERING_ALGORITME,BESKRIVELSE,CRON_EXPRESSION) 
values ('abakus.async.kopiergrunnlag','Abakus robust kopierng av grunnlag',1,30,'DEFAULT','Async kopering av grunnlag i abakus',null);



