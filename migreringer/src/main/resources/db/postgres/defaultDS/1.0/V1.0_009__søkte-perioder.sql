create table if not exists FO_FORDELING
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists FO_FORDELING_PERIODE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    FORDELING_ID  BIGINT REFERENCES FO_FORDELING (id),
    FOM           DATE                                   NOT NULL,
    TOM           DATE                                   NOT NULL,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create sequence if not exists SEQ_FO_FORDELING_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_FO_FORDELING increment by 50 minvalue 1000000;
create sequence if not exists SEQ_GR_FORDELING increment by 50 minvalue 1000000;

create index IDX_FO_FORDELING_PERIODE_1
    on FO_FORDELING_PERIODE (FORDELING_ID);

create table GR_FORDELING
(
    ID                   bigint                              not null PRIMARY KEY,
    BEHANDLING_ID        bigint                              not null,
    oppgitt_fordeling_id bigint                              not null,
    VERSJON              bigint       default 0              not null,
    AKTIV                boolean      default true           not null,
    OPPRETTET_AV         VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID        TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV            VARCHAR(20),
    ENDRET_TID           TIMESTAMP(3),
    constraint FK_GR_FORDELING_1
        foreign key (BEHANDLING_ID) references behandling,
    constraint FK_GR_FORDELING_2
        foreign key (oppgitt_fordeling_id) references FO_FORDELING
);

create index IDX_GR_FORDELING_1
    on GR_FORDELING (BEHANDLING_ID);
create index IDX_GR_FORDELING_2
    on GR_FORDELING (oppgitt_fordeling_id);

CREATE UNIQUE INDEX UIDX_GR_FORDELING_01
    ON GR_FORDELING (
                     (CASE
                          WHEN AKTIV = true
                              THEN BEHANDLING_ID
                          ELSE NULL END),
                     (CASE
                          WHEN AKTIV = true
                              THEN AKTIV
                          ELSE NULL END)
        );
