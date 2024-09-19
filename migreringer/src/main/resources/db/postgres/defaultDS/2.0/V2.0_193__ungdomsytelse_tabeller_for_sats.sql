create table if not exists UNG_SATS_PERIODER
(
    ID                          BIGINT                                      NOT NULL PRIMARY KEY,
    OPPRETTET_AV                VARCHAR(20)     DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID               TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    ENDRET_AV                   VARCHAR(20)                                         ,
    ENDRET_TID                  TIMESTAMP(3)
);

create table if not exists UNG_SATS_PERIODE
(
    ID                          BIGINT                                      NOT NULL PRIMARY KEY,
    ung_sats_perioder_id        BIGINT                                      not null,
    periode                     daterange                                   not null,
    dagsats                     numeric(19, 4)                              not null,
    grunnbeløp                  numeric(12, 2)                              not null,
    grunnbeløp_faktor           numeric(19, 4)                              not null,
    OPPRETTET_AV                VARCHAR(20)     DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID               TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    ENDRET_AV                   VARCHAR(20)                                         ,
    ENDRET_TID                  TIMESTAMP(3)                                        ,

    constraint FK_UNG_GR_UNG_SATS_PERIODER foreign key (ung_sats_perioder_id) references UNG_SATS_PERIODER,
    constraint UNG_SATS_PERIODE_IKKE_OVERLAPP EXCLUDE USING GIST (ung_sats_perioder_id WITH =, periode WITH &&)
    );

create table if not exists  UNG_GR(
    ID BIGINT NOT NULL,
    BEHANDLING_ID BIGINT NOT NULL,
    ung_sats_perioder_id BIGINT not null,
    VERSJON BIGINT DEFAULT 0 NOT NULL,
    OPPRETTET_AV VARCHAR(20) DEFAULT 'VL' NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV VARCHAR(20),
    ENDRET_TID TIMESTAMP(3),
    AKTIV BOOLEAN NOT NULL,

    constraint FK_UNG_GR_UNG_SATS_PERIODER foreign key (ung_sats_perioder_id) references UNG_SATS_PERIODER,
    constraint FK_UNG_GR_BEHANDLING foreign key (behandling_id) references BEHANDLING
    );

create sequence if not exists SEQ_UNG_GR increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNG_SATS_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNG_SATS_PERIODER increment by 50 minvalue 1000000;

create unique index UIDX_UNG_GR_1 on UNG_GR (behandling_id) where aktiv = true;
create  index IDX_UNG_SATS_PERIODE_PERIODER on UNG_SATS_PERIODE (ung_sats_perioder_id);
