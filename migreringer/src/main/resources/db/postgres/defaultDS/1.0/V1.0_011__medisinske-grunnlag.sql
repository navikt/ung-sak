create table if not exists MD_LEGEERKLAERINGER
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists MD_LEGEERKLAERING
(
    ID                  BIGINT                                 NOT NULL PRIMARY KEY,
    LEGEERKLAERINGER_ID BIGINT REFERENCES MD_LEGEERKLAERINGER (id),
    referanse           uuid                                   not null,
    FOM                 DATE                                   NOT NULL,
    TOM                 DATE                                   NOT NULL,
    DIAGNOSE            VARCHAR(100)                           NOT NULL,
    KILDE               VARCHAR(100)                           NOT NULL,
    VERSJON             BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV        VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID       TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV           VARCHAR(20),
    ENDRET_TID          TIMESTAMP(3)
);
create index IDX_MD_LEGEERKLAERING_1
    on MD_LEGEERKLAERING (LEGEERKLAERINGER_ID);

create table if not exists MD_INNLEGGELSE
(
    ID                BIGINT                                 NOT NULL PRIMARY KEY,
    LEGEERKLAERING_ID BIGINT REFERENCES MD_LEGEERKLAERING (id),
    FOM               DATE                                   NOT NULL,
    TOM               DATE                                   NOT NULL,
    VERSJON           BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV      VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID     TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV         VARCHAR(20),
    ENDRET_TID        TIMESTAMP(3)
);
create index IDX_MD_INNLEGGELSE_1
    on MD_INNLEGGELSE (LEGEERKLAERING_ID);

create table if not exists MD_KONTINUERLIG_TILSYN
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists MD_KONTINUERLIG_TILSYN_PERIODE
(
    ID                     BIGINT                                 NOT NULL PRIMARY KEY,
    KONTINUERLIG_TILSYN_ID BIGINT REFERENCES MD_KONTINUERLIG_TILSYN (id),
    FOM                    DATE                                   NOT NULL,
    TOM                    DATE                                   NOT NULL,
    GRAD                   INT                                    NOT NULL,
    BEGRUNNELSE            VARCHAR(4000)                          NOT NULL,
    VERSJON                BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV           VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID          TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3)
);
create index IDX_MD_KONTINUERLIG_TILSYN_PERIODE_1
    on MD_KONTINUERLIG_TILSYN_PERIODE (KONTINUERLIG_TILSYN_ID);

create sequence if not exists SEQ_MD_KONTINUERLIG_TILSYN_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_MD_KONTINUERLIG_TILSYN increment by 50 minvalue 1000000;
create sequence if not exists SEQ_MD_LEGEERKLAERINGER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_MD_LEGEERKLAERING increment by 50 minvalue 1000000;
create sequence if not exists SEQ_MD_INNLEGGELSE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_GR_MEDISINSK increment by 50 minvalue 1000000;


create table GR_MEDISINSK
(
    ID                     bigint                              not null PRIMARY KEY,
    BEHANDLING_ID          bigint                              not null,
    legeerklaeringer_id    bigint,
    kontinuerlig_tilsyn_id bigint,
    VERSJON                bigint       default 0              not null,
    AKTIV                  boolean      default true           not null,
    OPPRETTET_AV           VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID          TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3),
    constraint FK_GR_MEDISINSK_1
        foreign key (BEHANDLING_ID) references BEHANDLING,
    constraint FK_GR_MEDISINSK_2
        foreign key (legeerklaeringer_id) references MD_LEGEERKLAERINGER,
    constraint FK_GR_MEDISINSK_3
        foreign key (kontinuerlig_tilsyn_id) references MD_KONTINUERLIG_TILSYN
);

create index IDX_GR_MEDISINSK_1
    on GR_MEDISINSK (BEHANDLING_ID);
create index IDX_GR_MEDISINSK_2
    on GR_MEDISINSK (legeerklaeringer_id);
create index IDX_GR_MEDISINSK_3
    on GR_MEDISINSK (kontinuerlig_tilsyn_id);

CREATE UNIQUE INDEX UIDX_GR_MEDISINSK_01
    ON GR_MEDISINSK (
                     (CASE
                          WHEN AKTIV = true
                              THEN BEHANDLING_ID
                          ELSE NULL END),
                     (CASE
                          WHEN AKTIV = true
                              THEN AKTIV
                          ELSE NULL END)
        );
