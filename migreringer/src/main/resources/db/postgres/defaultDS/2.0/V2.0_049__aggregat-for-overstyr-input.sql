create table SAK_INFOTRYGD_MIGRERING
(
    ID                          bigint                              not null PRIMARY KEY,
    FAGSAK_ID                   bigint                              not null,
    SKJAERINGSTIDSPUNKT         DATE                                not null,
    VERSJON                     bigint       default 0              not null,
    OPPRETTET_AV                VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID               TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV                   VARCHAR(20),
    ENDRET_TID                  TIMESTAMP(3),
    constraint FK_SAK_INFOTRYGD_MIGRERING_01
        foreign key (FAGSAK_ID) references FAGSAK(ID)
);
create index IDX_SAK_INFOTRYGD_MIGRERING_01 on SAK_INFOTRYGD_MIGRERING (FAGSAK_ID);

CREATE UNIQUE INDEX UIDX_SAK_INFOTRYGD_MIGRERING_01 ON SAK_INFOTRYGD_MIGRERING (FAGSAK_ID, SKJAERINGSTIDSPUNKT);

create sequence if not exists SEQ_SAK_INFOTRYGD_MIGRERING increment by 50 minvalue 1000000;

create table BG_OVST_INPUT_PERIODER
(
    ID            bigint                              not null PRIMARY KEY,
    VERSJON       bigint       default 0              not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create sequence if not exists SEQ_BG_OVST_INPUT_PERIODER increment by 50 minvalue 1000000;


create table BG_OVST_INPUT_PERIODE
(
    ID                          bigint                              not null PRIMARY KEY,
    BG_OVST_INPUT_ID            bigint                              not null,
    SKJAERINGSTIDSPUNKT         DATE                                not null,
    VERSJON                     bigint       default 0              not null,
    OPPRETTET_AV                VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID               TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV                   VARCHAR(20),
    ENDRET_TID                  TIMESTAMP(3),
    constraint FK_BG_OVST_INPUT_PERIODE_01
        foreign key (BG_OVST_INPUT_ID) references BG_OVST_INPUT_PERIODER(ID)
);
create index IDX_BG_OVST_INPUT_PERIODE_01 on BG_OVST_INPUT_PERIODE (BG_OVST_INPUT_ID);

CREATE UNIQUE INDEX UIDX_BG_OVST_INPUT_PERIODE_01 ON BG_OVST_INPUT_PERIODE (BG_OVST_INPUT_ID, SKJAERINGSTIDSPUNKT);


create sequence if not exists SEQ_BG_OVST_INPUT_PERIODE increment by 50 minvalue 1000000;

create table BG_OVST_AKTIVITET
(
    ID                              bigint                              not null PRIMARY KEY,
    BG_OVST_INPUT_PERIODE_ID        bigint                              not null,
    ARBEIDSGIVER_AKTOR_ID           VARCHAR(100),
    ARBEIDSGIVER_ORGNR              VARCHAR(100),
    INNTEKT_PR_AAR                  NUMERIC(19, 2)                      not null,
    REFUSJON_PR_AAR                 NUMERIC(19, 2),
    AKTIVITET_STATUS                VARCHAR(20)                         not null,
    FOM                             DATE                                not null,
    TOM                             DATE                                not null,
    VERSJON                         bigint       default 0              not null,
    OPPRETTET_AV                    VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID                   TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV                       VARCHAR(20),
    ENDRET_TID                      TIMESTAMP(3),
    constraint FK_BG_OVST_AKTIVITET_01
        foreign key (BG_OVST_INPUT_PERIODE_ID) references BG_OVST_INPUT_PERIODE(ID)
);
create index IDX_BG_OVST_AKTIVITET_01 on BG_OVST_AKTIVITET (BG_OVST_INPUT_PERIODE_ID);

create sequence if not exists SEQ_BG_OVST_AKTIVITET increment by 50 minvalue 1000000;

ALTER TABLE gr_beregningsgrunnlag add column bg_ovst_input_id bigint;
ALTER TABLE gr_beregningsgrunnlag add constraint FK_gr_beregningsgrunnlag_10 foreign key (bg_ovst_input_id) references BG_OVST_INPUT_PERIODER;
