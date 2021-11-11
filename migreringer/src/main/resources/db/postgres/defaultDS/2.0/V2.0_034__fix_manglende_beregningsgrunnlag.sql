Insert into PROSESS_TASK_TYPE 
(KODE,NAVN,FEIL_MAKS_FORSOEK,FEIL_SEK_MELLOM_FORSOEK,FEILHANDTERING_ALGORITME,BESKRIVELSE,CRON_EXPRESSION) 
values 
('fix.psbFeilrette','Task for feilretting av PSB-saker',1,60,'DEFAULT','Task for feilretting av PSB-saker',null);

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'),
  'fix.psbFeilrette',
  nextval('seq_prosess_task_gruppe'),
  current_timestamp at time zone 'UTC' + interval '5 minutes',
  'saksnummer=' || f.saksnummer
FROM Fagsak f
WHERE f.ytelse_type = 'PSB';