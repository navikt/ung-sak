create table if not exists OLP_VURDERT_OPPLAERING_HOLDER
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3)
);
create sequence if not exists SEQ_OLP_VURDERT_OPPLAERING_HOLDER increment by 50 minvalue 1000000;

create table if not exists OLP_VURDERT_INSTITUSJON_HOLDER
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3)
);
create sequence if not exists SEQ_OLP_VURDERT_INSTITUSJON_HOLDER increment by 50 minvalue 1000000;

create table if not exists GR_OPPLAERING
(
    ID                              BIGINT                                  NOT NULL PRIMARY KEY,
    BEHANDLING_ID                   BIGINT                                  NOT NULL,
    VURDERT_INSTITUSJON_HOLDER_ID   BIGINT                                  NOT NULL,
    VURDERT_OPPLAERING_HOLDER_ID    BIGINT                                  NOT NULL,
    AKTIV                           BOOLEAN      DEFAULT false              NOT NULL,
    BEGRUNNELSE                     VARCHAR(4000)                                   ,
    VERSJON                         BIGINT       DEFAULT 0                  NOT NULL,
    OPPRETTET_AV                    VARCHAR(20)  DEFAULT 'VL'               NOT NULL,
    OPPRETTET_TID                   TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    ENDRET_AV                       VARCHAR(20),
    ENDRET_TID                      TIMESTAMP(3),
    constraint FK_GR_OPPLAERING_1 foreign key (BEHANDLING_ID) references BEHANDLING,
    constraint FK_GR_OPPLAERING_2 foreign key (VURDERT_INSTITUSJON_HOLDER_ID) references OLP_VURDERT_INSTITUSJON_HOLDER,
    constraint FK_GR_OPPLAERING_3 foreign key (VURDERT_OPPLAERING_HOLDER_ID) references OLP_VURDERT_OPPLAERING_HOLDER
);
create sequence if not exists SEQ_GR_OPPLAERING increment by 50 minvalue 1000000;
create index IDX_GR_OPPLAERING_1 on GR_OPPLAERING (BEHANDLING_ID);
create unique index UIDX_GR_OPPLAERING_1 ON GR_OPPLAERING (BEHANDLING_ID) WHERE (AKTIV = TRUE);
create index IDX_GR_OPPLAERING_2 on GR_OPPLAERING (VURDERT_INSTITUSJON_HOLDER_ID);
create index IDX_GR_OPPLAERING_3 on GR_OPPLAERING (VURDERT_OPPLAERING_HOLDER_ID);

create table if not exists OLP_VURDERT_OPPLAERING
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID               BIGINT                                 NOT NULL,
    FOM                     DATE                                   NOT NULL,
    TOM                     DATE                                   NOT NULL,
    NOEDVENDIG_OPPLAERING   BOOLEAN      DEFAULT false             NOT NULL,
    INSTITUSJON             VARCHAR(100)                           NOT NULL,
    BEGRUNNELSE             VARCHAR(4000)                                  ,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3),
    constraint FK_OLP_VURDERT_OPPLAERING_1 foreign key (HOLDER_ID) references OLP_VURDERT_OPPLAERING_HOLDER
);
create sequence if not exists SEQ_OLP_VURDERT_OPPLAERING increment by 50 minvalue 1000000;
create index IDX_OLP_VURDERT_OPPLAERING_1 on OLP_VURDERT_OPPLAERING (HOLDER_ID);

create table if not exists OLP_VURDERT_INSTITUSJON
(
    ID                      BIGINT                                  NOT NULL PRIMARY KEY,
    HOLDER_ID               BIGINT                                  NOT NULL,
    INSTITUSJON             VARCHAR(100)                            NOT NULL,
    GODKJENT                BOOLEAN      DEFAULT false              NOT NULL,
    BEGRUNNELSE             VARCHAR(4000)                                   ,
    VERSJON                 BIGINT       DEFAULT 0                  NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'               NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3),
    constraint FK_OLP_VURDERT_INSTITUSJON_1 foreign key (HOLDER_ID) references OLP_VURDERT_INSTITUSJON_HOLDER
);
create sequence if not exists SEQ_OLP_VURDERT_INSTITUSJON increment by 50 minvalue 1000000;
create index IDX_OLP_VURDERT_INSTITUSJON_1 on OLP_VURDERT_INSTITUSJON (HOLDER_ID);
