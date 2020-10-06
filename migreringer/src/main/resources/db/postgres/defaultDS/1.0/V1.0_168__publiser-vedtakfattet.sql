INSERT INTO PROSESS_TASK_TYPE (KODE,
                               NAVN,
                               FEIL_MAKS_FORSOEK,
                               FEIL_SEK_MELLOM_FORSOEK,
                               FEILHANDTERING_ALGORITME,
                               BESKRIVELSE)
VALUES ('vedtak.publiserVedtakhendelse',
        'Publiser ethvert fattet vedtak på kafka',
        3,
        60,
        'DEFAULT',
        'Publiserer vedtak på kø.');


ALTER TABLE BEHANDLING_VEDTAK ADD column ER_PUBLISERT boolean default false not null;


INSERT INTO PROSESS_TASK_TYPE (KODE,
                               NAVN,
                               FEIL_MAKS_FORSOEK,
                               FEIL_SEK_MELLOM_FORSOEK,
                               FEILHANDTERING_ALGORITME,
                               BESKRIVELSE)


VALUES ('vedtak.etterfyllHistoriske',
        'Etterfyller historiske vedtakhendelser',
        3,
        60,
        'DEFAULT',
        'Publiserer allerede fattede vedtak på kø.');


-- Starter task for etterfylling av vedtakhendelser --
insert into prosess_task (id, task_type, prioritet, status, task_gruppe, task_sekvens, partition_key)
values (nextval('SEQ_PROSESS_TASK'), 'vedtak.etterfyllHistoriske', 50, 'KLAR', nextval('SEQ_PROSESS_TASK_GRUPPE'), 1, '10');
