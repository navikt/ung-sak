create table if not exists UP_BEREDKSAP_PERIODE
(
    ID               BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID        BIGINT REFERENCES UP_SOEKNAD_PERIODER (id),
    FOM              DATE                                   NOT NULL,
    TOM              DATE                                   NOT NULL,
    VIL_HA_BEREDSKAP BOOLEAN                             NOT NULL,
    BESKRIVELSE      VARCHAR(4000)                          ,
    VERSJON          BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV     VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV        VARCHAR(20),
    ENDRET_TID       TIMESTAMP(3)
);
create index IDX_UP_BEREDSKAP_PERIODE_01 on UP_BEREDSKAP_PERIODE (HOLDER_ID);
create sequence if not exists SEQ_UP_BEREDSKAP_PERIODE increment by 50 minvalue 1000000;


create table if not exists UP_NATTEVAAK_PERIODE
(
    ID               BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID        BIGINT REFERENCES UP_SOEKNAD_PERIODER (id),
    FOM              DATE                                   NOT NULL,
    TOM              DATE                                   NOT NULL,
    VIL_HA_NATTEVAAK BOOLEAN                             NOT NULL,
    BESKRIVELSE      VARCHAR(4000)                          ,
    VERSJON          BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV     VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV        VARCHAR(20),
    ENDRET_TID       TIMESTAMP(3)
);
create index IDX_UP_NATTEVAAK_PERIODE_01 on UP_NATTEVAAK_PERIODE (HOLDER_ID);
create sequence if not exists SEQ_UP_NATTEVAAK_PERIODE increment by 50 minvalue 1000000;
