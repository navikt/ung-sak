create table if not exists OLP_GODKJENT_INSTITUSJON
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    NAVN                    VARCHAR(100)                           NOT NULL,
    FOM                     DATE                                   ,
    TOM                     DATE                                   ,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3)
);
create sequence if not exists SEQ_OLP_GODKJENT_INSTITUSJON increment by 50 minvalue 1000000;
create unique index UIDX_OLP_GODKJENT_INSTITUSJON_1 ON OLP_GODKJENT_INSTITUSJON (NAVN);
