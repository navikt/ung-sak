create sequence if not exists SEQ_GR_UNNTAK_ETABLERT_TILSYN increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNNTAK_ETABLERT_TILSYN increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNNTAK_ETABLERT_TILSYN_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNNTAK_ETABLERT_TILSYN_BESKRIVELSE increment by 50 minvalue 1000000;

create table if not exists UNNTAK_ETABLERT_TILSYN
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,

    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE
(
    ID                          BIGINT                   NOT NULL PRIMARY KEY,
    PLEIETRENGENDE_AKTOER_ID    VARCHAR(50)              NOT NULL,
    BEREDSKAP_ID                BIGINT                   ,
    NATTEVAAK_ID                BIGINT                   ,

    VERSJON                     BIGINT DEFAULT 0         NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    constraint FK_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_1 foreign key (BEREDSKAP_ID) references UNNTAK_ETABLERT_TILSYN,
    constraint FK_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_2 foreign key (NATTEVAAK_ID) references UNNTAK_ETABLERT_TILSYN
);

create index IDX_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_1 ON UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE (PLEIETRENGENDE_AKTOER_ID);
create index IDX_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_2 ON UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE (BEREDSKAP_ID);
create index IDX_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_3 ON UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE (NATTEVAAK_ID);

create table if not exists GR_UNNTAK_ETABLERT_TILSYN
(
    ID                                          BIGINT                                 NOT NULL PRIMARY KEY,
    BEHANDLING_ID                               BIGINT                                 NOT NULL,
    UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_ID    BIGINT                                 NOT NULL,

    VERSJON                                     BIGINT       DEFAULT 0                 NOT NULL,
    AKTIV                                       BOOLEAN      DEFAULT TRUE              NOT NULL,
    OPPRETTET_AV                                VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID                               TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV                                   VARCHAR(20),
    ENDRET_TID                                  TIMESTAMP(3),
--    constraint FK_GR_UNNTAK_ETABLERT_TILSYN_1 foreign key (BEHANDLING_ID) references BEHANDLING,
    constraint FK_GR_UNNTAK_ETABLERT_TILSYN_2 foreign key (UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_ID) references UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE
);

create index IDX_GR_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_1 on GR_UNNTAK_ETABLERT_TILSYN (BEHANDLING_ID);
create index IDX_GR_UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_2 on GR_UNNTAK_ETABLERT_TILSYN (UNNTAK_ETABLERT_TILSYN_PLEIETRENGENDE_ID);


create table if not exists UNNTAK_ETABLERT_TILSYN_PERIODE
(
    ID                          BIGINT                                          NOT NULL PRIMARY KEY,
    UNNTAK_ETABLERT_TILSYN_ID   BIGINT REFERENCES UNNTAK_ETABLERT_TILSYN (id)   NOT NULL,
    FOM                         DATE                                            NOT NULL,
    TOM                         DATE                                            NOT NULL,
    BEGRUNNELSE                 VARCHAR(4000)                                   ,
    RESULTAT                    VARCHAR(20)                                     NOT NULL,
    KILDE_BEHANDLING_ID         BIGINT                                          NOT NULL,

    VERSJON                     BIGINT       DEFAULT 0                          NOT NULL,
    OPPRETTET_AV                VARCHAR(20)  DEFAULT 'VL'                       NOT NULL,
    OPPRETTET_TID               TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP          NOT NULL,
    ENDRET_AV                   VARCHAR(20)                                     ,
    ENDRET_TID                  TIMESTAMP(3)
);

create index IDX_UNNTAK_ETABLERT_TILSYN_PERIODE_1 on UNNTAK_ETABLERT_TILSYN_PERIODE (UNNTAK_ETABLERT_TILSYN_ID);

create table UNNTAK_ETABLERT_TILSYN_BESKRIVELSE
(
    ID                          BIGINT                                          NOT NULL PRIMARY KEY,
    UNNTAK_ETABLERT_TILSYN_ID   BIGINT REFERENCES UNNTAK_ETABLERT_TILSYN (id)   NOT NULL,
    FOM                         DATE                                            NOT NULL,
    TOM                         DATE                                            NOT NULL,
    TEKST                       VARCHAR(4000)                                   ,
    MOTTATT_DATO                DATE                                            NOT NULL,
    SOEKER_AKTOER_ID            VARCHAR(50)                                     NOT NULL,
    KILDE_BEHANDLING_ID         BIGINT                                          NOT NULL,

    VERSJON                     BIGINT       default 0                          NOT NULL,
    OPPRETTET_AV                VARCHAR(20)  default 'VL'                       NOT NULL,
    OPPRETTET_TID               TIMESTAMP(3) default localtimestamp             NOT NULL,
    ENDRET_AV                   VARCHAR(20),
    ENDRET_TID                  TIMESTAMP(3)
);

create index IDX_UNNTAK_ETABLERT_TILSYN_BESKRIVELSE_1 on UNNTAK_ETABLERT_TILSYN_BESKRIVELSE (UNNTAK_ETABLERT_TILSYN_ID);
