create table BG_NAERING_INNTEKT_PERIODER
(
    ID            bigint                              not null PRIMARY KEY,
    VERSJON       bigint       default 0              not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table BG_NAERING_INNTEKT_PERIODE
(
    ID                    bigint                              not null PRIMARY KEY,
    bg_nearing_inntekt_id bigint                              not null,
    skjaeringstidspunkt   DATE                                NOT NULL,
    iay_referanse         uuid                                NOT NULL,
    VERSJON               bigint       default 0              not null,
    OPPRETTET_AV          VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID         TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV             VARCHAR(20),
    ENDRET_TID            TIMESTAMP(3),
    constraint FK_BG_NAERING_INNTEKT_PERIODER_01
        foreign key (bg_nearing_inntekt_id) references BG_NAERING_INNTEKT_PERIODER
);
create index IDX_BG_NAERING_INNTEKT_PERIODE_01
    on BG_NAERING_INNTEKT_PERIODE (bg_nearing_inntekt_id);

CREATE UNIQUE INDEX UIDX_BG_NAERING_INNTEKT_PERIODE_01 ON BG_NAERING_INNTEKT_PERIODE (bg_nearing_inntekt_id, skjaeringstidspunkt);
CREATE UNIQUE INDEX UIDX_BG_NAERING_INNTEKT_PERIODE_02 ON BG_NAERING_INNTEKT_PERIODE (bg_nearing_inntekt_id, iay_referanse);

create sequence if not exists SEQ_BG_NAERING_INNTEKT_PERIODER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_BG_NAERING_INNTEKT_PERIODE increment by 50 minvalue 1000000;

ALTER TABLE gr_beregningsgrunnlag
    add column bg_nearing_inntekt_id bigint;
ALTER TABLE gr_beregningsgrunnlag
    add constraint FK_gr_beregningsgrunnlag_11 foreign key (bg_nearing_inntekt_id) references BG_NAERING_INNTEKT_PERIODER;
