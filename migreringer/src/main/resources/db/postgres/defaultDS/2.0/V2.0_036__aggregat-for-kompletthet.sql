create table BG_KOMPLETT_PERIODER
(
    ID            bigint                              not null PRIMARY KEY,
    VERSJON       bigint       default 0              not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table BG_KOMPLETT_PERIODE
(
    ID                  bigint                              not null PRIMARY KEY,
    bg_komplett_id      bigint                              not null,
    skjaeringstidspunkt DATE                                NOT NULL,
    vurdering           VARCHAR(20)                         NOT NULL,
    BEGRUNNELSE         VARCHAR(2000),
    VERSJON             bigint       default 0              not null,
    OPPRETTET_AV        VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID       TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV           VARCHAR(20),
    ENDRET_TID          TIMESTAMP(3),
    constraint FK_BG_KOMPLETT_PERIODER_01
        foreign key (bg_komplett_id) references BG_KOMPLETT_PERIODER
);
create index IDX_BG_KOMPLETT_PERIODE_01
    on BG_KOMPLETT_PERIODE (bg_komplett_id);

CREATE UNIQUE INDEX UIDX_BG_KOMPLETT_PERIODE_01 ON BG_KOMPLETT_PERIODE (bg_komplett_id, skjaeringstidspunkt);
CREATE UNIQUE INDEX UIDX_BG_KOMPLETT_PERIODE_02 ON BG_KOMPLETT_PERIODE (bg_komplett_id, vurdering);

create sequence if not exists SEQ_BG_KOMPLETT_PERIODER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_BG_KOMPLETT_PERIODE increment by 50 minvalue 1000000;

ALTER TABLE gr_beregningsgrunnlag add column bg_komplett_id bigint;
ALTER TABLE gr_beregningsgrunnlag add constraint FK_gr_beregningsgrunnlag_9 foreign key (bg_komplett_id) references BG_KOMPLETT_PERIODER;
ALTER TABLE gr_beregningsgrunnlag alter column bg_grunnlag_id DROP NOT NULL;
