create table if not exists BG_PGI_PERIODER
(
    ID            bigint                              not null PRIMARY KEY,
    VERSJON       bigint       default 0              not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists BG_PGI_PERIODE
(
    ID                    bigint                              not null PRIMARY KEY,
    bg_pgi_id             bigint                              not null,
    skjaeringstidspunkt   DATE                                NOT NULL,
    iay_referanse         uuid                                NOT NULL,
    VERSJON               bigint       default 0              not null,
    OPPRETTET_AV          VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID         TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV             VARCHAR(20),
    ENDRET_TID            TIMESTAMP(3),
    constraint FK_BG_PGI_PERIODER_01
        foreign key (bg_pgi_id) references BG_PGI_PERIODER
);
create index if not exists IDX_BG_PGI_PERIODE_01
    on BG_PGI_PERIODE (bg_pgi_id);

CREATE UNIQUE INDEX if not exists UIDX_BG_PGI_PERIODE_01 ON BG_PGI_PERIODE (bg_pgi_id, skjaeringstidspunkt);
CREATE UNIQUE INDEX if not exists UIDX_BG_PGI_PERIODE_02 ON BG_PGI_PERIODE (bg_pgi_id, iay_referanse);

create sequence if not exists SEQ_BG_PGI_PERIODER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_BG_PGI_PERIODE increment by 50 minvalue 1000000;

ALTER TABLE gr_beregningsgrunnlag
    add column bg_pgi_id bigint;
ALTER TABLE gr_beregningsgrunnlag
    add constraint FK_gr_beregningsgrunnlag_12 foreign key (bg_pgi_id) references BG_PGI_PERIODER;
