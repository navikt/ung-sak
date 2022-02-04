insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'),
  'init.fullPubliseringAvBrukerdialoginnsyn',
  nextval('seq_prosess_task_gruppe'),
  current_timestamp at time zone 'UTC' + interval '5 minutes',
  ''
;