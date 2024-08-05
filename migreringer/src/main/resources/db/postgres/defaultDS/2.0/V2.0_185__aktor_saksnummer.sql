create table if not exists SAKSNUMMER_AKTOR
(
    ID              BIGINT                                      NOT NULL PRIMARY KEY,
    SAKSNUMMER      VARCHAR(19)                                 NOT NULL,
    AKTOER_ID       VARCHAR(50)                                 NOT NULL,
    JOURNALPOST_ID  VARCHAR(50)                                 NOT NULL,
    SLETTET         BOOLEAN         DEFAULT false               NOT NULL,
    OPPRETTET_AV    VARCHAR(20)     DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID   TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    ENDRET_AV       VARCHAR(20)                                         ,
    ENDRET_TID      TIMESTAMP(3)
);
create sequence if not exists SEQ_SAKSNUMMER_AKTOR increment by 50 minvalue 1000000;

create index IDX_SAKSNUMMER_AKTOR_1 on SAKSNUMMER_AKTOR (SAKSNUMMER);
create index IDX_SAKSNUMMER_AKTOR_2 on SAKSNUMMER_AKTOR (JOURNALPOST_ID);

create unique index UIDX_SAKSNUMMER_AKTOR_1 on SAKSNUMMER_AKTOR (SAKSNUMMER, AKTOER_ID, JOURNALPOST_ID) WHERE (SLETTET = false);
create unique index UIDX_SAKSNUMMER_AKTOR_2 on SAKSNUMMER_AKTOR (AKTOER_ID, JOURNALPOST_ID) WHERE (SLETTET = false);

