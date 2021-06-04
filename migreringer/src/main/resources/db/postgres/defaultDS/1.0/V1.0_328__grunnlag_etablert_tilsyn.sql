create table if not exists ETABLERT_TILSYN
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists ETABLERT_TILSYN_PERIODE
(
    ID                     BIGINT                                 NOT NULL PRIMARY KEY,
    ETABLERT_TILSYN_ID        BIGINT REFERENCES ETABLERT_TILSYN (id),
    FOM                    DATE                                   NOT NULL,
    TOM                    DATE                                   NOT NULL,
    JOURNALPOST_ID         VARCHAR(20)                            NOT NULL,
    VARIGHET			   VARCHAR(20)                            NOT NULL,
    
    VERSJON                BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV           VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID          TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3)
);

create index IDX_ETABLERT_TILSYN_PERIODE_1
    on ETABLERT_TILSYN_PERIODE (ETABLERT_TILSYN_ID);


create table GR_ETABLERT_TILSYN
(
    ID                     bigint                              not null PRIMARY KEY,
    BEHANDLING_ID          bigint                              not null,
    ETABLERT_TILSYN_ID    bigint,
    VERSJON                bigint       default 0              not null,
    AKTIV                  boolean      default true           not null,
    OPPRETTET_AV           VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID          TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3),
    constraint FK_GR_ETABLERT_TILSYN_1
        foreign key (BEHANDLING_ID) references BEHANDLING,
    constraint FK_GR_ETABLERT_TILSYN_2
        foreign key (ETABLERT_TILSYN_ID) references ETABLERT_TILSYN
);

create index IDX_GR_ETABLERT_TILSYN_1
    on GR_ETABLERT_TILSYN (BEHANDLING_ID);
create index IDX_GR_ETABLERT_TILSYN_2
    on GR_ETABLERT_TILSYN (ETABLERT_TILSYN_ID);
CREATE UNIQUE INDEX UIDX_GR_ETABLERT_TILSYN_01
    ON GR_ETABLERT_TILSYN (
                     (CASE
                          WHEN AKTIV = true
                              THEN BEHANDLING_ID
                          ELSE NULL END),
                     (CASE
                          WHEN AKTIV = true
                              THEN AKTIV
                          ELSE NULL END)
        );

create sequence if not exists SEQ_GR_ETABLERT_TILSYN increment by 50 minvalue 1000000;
create sequence if not exists SEQ_ETABLERT_TILSYN increment by 50 minvalue 1000000;
create sequence if not exists SEQ_ETABLERT_TILSYN_PERIODE increment by 50 minvalue 1000000;
