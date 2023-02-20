Insert into PROSESS_TASK_TYPE
(KODE, NAVN, FEIL_MAKS_FORSOEK, FEIL_SEK_MELLOM_FORSOEK, FEILHANDTERING_ALGORITME, BESKRIVELSE, CRON_EXPRESSION)
values ('fagsak.avsluttFagsak', 'Avsluttning av fagsak', 1, 60, 'DEFAULT',
        'Avsluttning av fagsak', null);
Insert into PROSESS_TASK_TYPE
(KODE, NAVN, FEIL_MAKS_FORSOEK, FEIL_SEK_MELLOM_FORSOEK, FEILHANDTERING_ALGORITME, BESKRIVELSE, CRON_EXPRESSION)
values ('batch.finnFagsakerForAvsluttning', 'Finner kandidater for avslutting', 1, 60, 'DEFAULT',
        'Finner kandidater for avslutting', '0 30 17 * * *');

insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
values (nextval('SEQ_PROSESS_TASK'), 'batch.finnFagsakerForAvsluttning', 1, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1, '05');
