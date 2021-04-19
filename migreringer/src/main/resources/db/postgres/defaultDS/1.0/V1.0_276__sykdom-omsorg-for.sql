
-- Fjerner gammel modell for sykdom og omsorgen-for:
drop table if exists MD_INNLEGGELSE;
drop table if exists MD_LEGEERKLAERING;
drop table if exists GR_MEDISINSK;
drop table if exists MD_OMSORGENFOR;
drop table if exists MD_KONTINUERLIG_TILSYN_PERIODE;
drop table if exists MD_KONTINUERLIG_TILSYN;
drop table if exists MD_LEGEERKLAERINGER;

drop sequence if exists SEQ_MD_KONTINUERLIG_TILSYN_PERIODE;
drop sequence if exists SEQ_MD_KONTINUERLIG_TILSYN;
drop sequence if exists SEQ_MD_LEGEERKLAERINGER;
drop sequence if exists SEQ_MD_LEGEERKLAERING;
drop sequence if exists SEQ_MD_INNLEGGELSE;
drop sequence if exists SEQ_GR_MEDISINSK;
drop sequence if exists SEQ_MD_OMSORGENFOR;


-- Ny modell:

create table if not exists OMSORGEN_FOR
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists OMSORGEN_FOR_PERIODE
(
    ID                     BIGINT                                 NOT NULL PRIMARY KEY,
    OMSORGEN_FOR_ID        BIGINT REFERENCES OMSORGEN_FOR (id),
    FOM                    DATE                                   NOT NULL,
    TOM                    DATE                                   NOT NULL,
    RELASJON               VARCHAR(20)                            NOT NULL,
    RELASJONSBESKRIVELSE   VARCHAR(4000)                          NOT NULL,
    BEGRUNNELSE            VARCHAR(4000)                          ,
    RESULTAT               VARCHAR(20)                            NOT NULL,
    
    VERSJON                BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV           VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID          TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3)
);

create index IDX_OMSORGEN_FOR_PERIODE_1
    on OMSORGEN_FOR_PERIODE (OMSORGEN_FOR_ID);


create table GR_OMSORGEN_FOR
(
    ID                     bigint                              not null PRIMARY KEY,
    BEHANDLING_ID          bigint                              not null,
    OMSORGEN_FOR_ID    bigint,
    VERSJON                bigint       default 0              not null,
    AKTIV                  boolean      default true           not null,
    OPPRETTET_AV           VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID          TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3),
    constraint FK_GR_OMSORGEN_FOR_1
        foreign key (BEHANDLING_ID) references BEHANDLING,
    constraint FK_GR_OMSORGEN_FOR_2
        foreign key (OMSORGEN_FOR_ID) references OMSORGEN_FOR
);

create index IDX_GR_OMSORGEN_FOR_1
    on GR_OMSORGEN_FOR (BEHANDLING_ID);
create index IDX_GR_OMSORGEN_FOR_2
    on GR_OMSORGEN_FOR (OMSORGEN_FOR_ID);
CREATE UNIQUE INDEX UIDX_GR_OMSORGEN_FOR_01
    ON GR_OMSORGEN_FOR (
                     (CASE
                          WHEN AKTIV = true
                              THEN BEHANDLING_ID
                          ELSE NULL END),
                     (CASE
                          WHEN AKTIV = true
                              THEN AKTIV
                          ELSE NULL END)
        );

create sequence if not exists SEQ_GR_OMSORGEN_FOR increment by 50 minvalue 1000000;
create sequence if not exists SEQ_OMSORGEN_FOR increment by 50 minvalue 1000000;
create sequence if not exists SEQ_OMSORGEN_FOR_PERIODE increment by 50 minvalue 1000000;
