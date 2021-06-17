
create sequence if not exists SEQ_PSB_RETT_PLEIEPENGER_VED_DOED increment by 50 minvalue 1000000;
create sequence if not exists SEQ_PSB_GR_RETT_PLEIEPENGER_VED_DOED increment by 50 minvalue 1000000;

create table psb_rett_pleiepenger_ved_doed (
    ID                  BIGINT                                 NOT NULL PRIMARY KEY,

    VURDERING           VARCHAR(4096)                          NOT NULL,
    RETT_VED_DOED_TYPE  VARCHAR(20)                            NOT NULL,

    VERSJON             BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV        VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID       TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV           VARCHAR(20),
    ENDRET_TID          TIMESTAMP(3)
);

create table psb_gr_rett_pleiepenger_ved_doed (
    ID                               BIGINT                                 NOT NULL PRIMARY KEY,

    BEHANDLING_ID                    BIGINT                                 NOT NULL,
    PSB_RETT_PLEIEPENGER_VED_DOED_ID BIGINT,

    AKTIV                            BOOLEAN      DEFAULT TRUE              NOT NULL,
    VERSJON                          BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV                     VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID                    TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV                        VARCHAR(20),
    ENDRET_TID                       TIMESTAMP(3),
    constraint FK_PSB_GR_RETT_PLEIEPENGER_VED_DOED_1 foreign key (PSB_RETT_PLEIEPENGER_VED_DOED_ID) references PSB_RETT_PLEIEPENGER_VED_DOED
);

create index IDX_PSB_GR_RETT_PLEIEPENGER_VED_DOED_1 on psb_gr_rett_pleiepenger_ved_doed (BEHANDLING_ID);
create index IDX_PSB_GR_RETT_PLEIEPENGER_VED_DOED_2 on psb_gr_rett_pleiepenger_ved_doed (PSB_RETT_PLEIEPENGER_VED_DOED_ID);
