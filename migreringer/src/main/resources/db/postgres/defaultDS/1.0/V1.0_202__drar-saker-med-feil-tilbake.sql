INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
values (nextval('seq_prosess_task'),
       'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'),
       current_timestamp at time zone 'UTC' + interval '5 minutes',
       'fagsakId=1032622
  behandlingId=1286082
  startSteg=PRECONDITION_BERGRUNN');


INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
values (nextval('seq_prosess_task'),
        'behandlingskontroll.tilbakeTilStart',
        nextval('seq_prosess_task_gruppe'),
        current_timestamp at time zone 'UTC' + interval '5 minutes',
        'fagsakId=1102422
   behandlingId=1173382
   startSteg=PRECONDITION_BERGRUNN');

INSERT INTO prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
values (nextval('seq_prosess_task'),
        'behandlingskontroll.tilbakeTilStart',
        nextval('seq_prosess_task_gruppe'),
        current_timestamp at time zone 'UTC' + interval '5 minutes',
        'fagsakId=1028403
   behandlingId=1029013
   startSteg=PRECONDITION_BERGRUNN');

