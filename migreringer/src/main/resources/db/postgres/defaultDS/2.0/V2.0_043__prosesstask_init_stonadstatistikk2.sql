Insert into PROSESS_TASK_TYPE 
(KODE,NAVN,FEIL_MAKS_FORSOEK,FEIL_SEK_MELLOM_FORSOEK,FEILHANDTERING_ALGORITME,BESKRIVELSE,CRON_EXPRESSION) 
values 
('init.fullPubliseringAvStonadstatistikk','Full initiell publisering av stonadstatistikk',1,60,'DEFAULT','Full initiell publisering av stonadstatistikk',null);

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'),
  'init.fullPubliseringAvStonadstatistikk',
  nextval('seq_prosess_task_gruppe'),
  current_timestamp at time zone 'UTC' + interval '15 minutes',
  ''
;