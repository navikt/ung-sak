create table if not exists INNGAAENDE_HENDELSE
(
    ID                       BIGINT                                 NOT NULL PRIMARY KEY,
    HENDELSE_ID              VARCHAR(100)                           NOT NULL,
    TYPE                     VARCHAR(100)                           NOT NULL,
    KILDE                    VARCHAR(20)                            NOT NULL,
    AKTOER_ID                VARCHAR(100)                           NOT NULL,
    MELDING_OPPRETTET        TIMESTAMP(3),
    PAYLOAD                  TEXT,
    HAANDTERT_STATUS         VARCHAR(100) DEFAULT 'MOTTATT',
    HAANDTERT_AV_HENDELSE_ID VARCHAR(100),
    OPPRETTET_AV             VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID            TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV                VARCHAR(20),
    ENDRET_TID               TIMESTAMP(3)
);
create sequence if not exists SEQ_INNGAAENDE_HENDELSE increment by 50 minvalue 1000000;
create index on INNGAAENDE_HENDELSE (AKTOER_ID);

Insert into PROSESS_TASK_TYPE (KODE,NAVN,FEIL_MAKS_FORSOEK,FEIL_SEK_MELLOM_FORSOEK,FEILHANDTERING_ALGORITME,BESKRIVELSE)
values ('k9.hendelseInnsendelse','Send inn hendelse til k9-sak',1,30,'DEFAULT','Send inn hendelse til k9-sak');

