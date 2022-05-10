create table if not exists UB_PERIODER
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists UB_PERIODE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    perioder_id   BIGINT REFERENCES UB_PERIODER (id),
    periode       daterange                              NOT NULL,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);
create index IDX_UB_PERIODE_1
    on UB_PERIODE (perioder_id);

create table UTSATT_BEHANDLING_AV
(
    ID            bigint                              not null PRIMARY KEY,
    BEHANDLING_ID bigint                              not null,
    perioder_id   bigint                              NOT NULL,
    VERSJON       bigint       default 0              not null,
    AKTIV         boolean      default true           not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    constraint FK_UTSATT_BEHANDLING_AV_1
        foreign key (BEHANDLING_ID) references BEHANDLING,
    constraint FK_UTSATT_BEHANDLING_AV_2
        foreign key (perioder_id) references UB_PERIODER
);

create sequence if not exists SEQ_UB_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UB_PERIODER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UTSATT_BEHANDLING_AV increment by 50 minvalue 1000000;

create index IDX_UTSATT_BEHANDLING_AV_1
    on UTSATT_BEHANDLING_AV (BEHANDLING_ID);

create index IDX_UTSATT_BEHANDLING_AV_2
    on UTSATT_BEHANDLING_AV (perioder_id);

CREATE UNIQUE INDEX UIDX_UTSATT_BEHANDLING_AV_01 ON UTSATT_BEHANDLING_AV (BEHANDLING_ID) WHERE (AKTIV = TRUE);
