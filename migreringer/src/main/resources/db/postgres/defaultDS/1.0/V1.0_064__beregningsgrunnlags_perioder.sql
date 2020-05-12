create table BG_PERIODER
(
    ID            bigint                              not null PRIMARY KEY,
    VERSJON       bigint       default 0              not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table BG_PERIODE
(
    ID                  bigint                              not null PRIMARY KEY,
    bg_grunnlag_id      bigint                              not null,
    skjaeringstidspunkt DATE                                NOT NULL,
    ekstern_referanse   uuid                                NOT NULL,
    VERSJON             bigint       default 0              not null,
    OPPRETTET_AV        VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID       TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV           VARCHAR(20),
    ENDRET_TID          TIMESTAMP(3),
    constraint FK_BG_PERIODER_01
        foreign key (bg_grunnlag_id) references BG_PERIODER
);
create index IDX_BG_PERIODE_01
    on BG_PERIODE (bg_grunnlag_id);

CREATE UNIQUE INDEX UIDX_BG_PERIODE_01 ON BG_PERIODE (bg_grunnlag_id, skjaeringstidspunkt);
CREATE UNIQUE INDEX UIDX_BG_PERIODE_02 ON BG_PERIODE (bg_grunnlag_id, ekstern_referanse);

create table gr_beregningsgrunnlag
(
    ID             bigint                              not null PRIMARY KEY,
    BEHANDLING_ID  bigint                              not null,
    bg_grunnlag_id bigint                              not null,
    VERSJON        bigint       default 0              not null,
    AKTIV          boolean      default true           not null,
    OPPRETTET_AV   VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID  TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV      VARCHAR(20),
    ENDRET_TID     TIMESTAMP(3),
    constraint FK_GR_BEREGNINGSGRUNNLAG_01
        foreign key (BEHANDLING_ID) references behandling,
    constraint FK_GR_BEREGNINGSGRUNNLAG_02
        foreign key (bg_grunnlag_id) references BG_PERIODER
);

create index IDX_GR_BEREGNINGSGRUNNLAG_01
    on GR_BEREGNINGSGRUNNLAG (BEHANDLING_ID);
create index IDX_GR_BEREGNINGSGRUNNLAG_02
    on GR_BEREGNINGSGRUNNLAG (bg_grunnlag_id);

CREATE UNIQUE INDEX UIDX_GR_BEREGNINGSGRUNNLAG_01 ON GR_BEREGNINGSGRUNNLAG (BEHANDLING_ID) WHERE (AKTIV = TRUE);

create sequence if not exists SEQ_GR_BEREGNINGSGRUNNLAG increment by 50 minvalue 1000000;
create sequence if not exists SEQ_BG_PERIODER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_BG_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_TT_BEREGNING increment by 50 minvalue 1000000;

create table TT_BEREGNING
(
    ID                  bigint                              not null PRIMARY KEY,
    bg_grunnlag_id      bigint                              not null,
    skjaeringstidspunkt DATE                                NOT NULL,
    VERSJON             bigint       default 0              not null,
    OPPRETTET_AV        VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID       TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV           VARCHAR(20),
    ENDRET_TID          TIMESTAMP(3),
    constraint FK_TT_BEREGNING_01
        foreign key (bg_grunnlag_id) references TOTRINNRESULTATGRUNNLAG
);
create index IDX_TT_BEREGNING_01
    on TT_BEREGNING (bg_grunnlag_id);
