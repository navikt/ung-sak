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
