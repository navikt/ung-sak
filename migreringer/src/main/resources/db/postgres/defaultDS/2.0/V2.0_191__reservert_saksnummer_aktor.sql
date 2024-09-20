create table if not exists RESERVERT_SAKSNUMMER_AKTOR
(
    ID                          BIGINT                                      NOT NULL PRIMARY KEY,
    AKTOER_ID                   VARCHAR(50)                                 NOT NULL,
    RESERVERT_SAKSNUMMER_ID     BIGINT                                      NOT NULL,
    OPPRETTET_AV                VARCHAR(20)     DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID               TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    ENDRET_AV                   VARCHAR(20)                                         ,
    ENDRET_TID                  TIMESTAMP(3)                                        ,
    constraint FK_RESERVERT_SAKSNUMMER_AKTOR_1 foreign key (RESERVERT_SAKSNUMMER_ID) references RESERVERT_SAKSNUMMER
);
create sequence if not exists SEQ_RESERVERT_SAKSNUMMER_AKTOR increment by 50 minvalue 1000000;

create unique index UIDX_RESERVERT_SAKSNUMMER_AKTOR_1 on RESERVERT_SAKSNUMMER_AKTOR (AKTOER_ID, RESERVERT_SAKSNUMMER_ID);
