create table if not exists RESERVERT_SAKSNUMMER
(
    ID                          BIGINT                                      NOT NULL PRIMARY KEY,
    SAKSNUMMER                  VARCHAR(19)                                 NOT NULL,
    YTELSE_TYPE                 VARCHAR(100)                                NOT NULL,
    BRUKER_AKTOER_ID            VARCHAR(50)                                 NOT NULL,
    PLEIETRENGENDE_AKTOER_ID    VARCHAR(50)                                         ,
    OPPRETTET_AV                VARCHAR(20)     DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID               TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    ENDRET_AV                   VARCHAR(20)                                         ,
    ENDRET_TID                  TIMESTAMP(3)
);
create sequence if not exists SEQ_RESERVERT_SAKSNUMMER increment by 50 minvalue 1000000;

create unique index UIDX_RESERVERT_SAKSNUMMER_1 on SAKSNUMMER_AKTOR (SAKSNUMMER);
