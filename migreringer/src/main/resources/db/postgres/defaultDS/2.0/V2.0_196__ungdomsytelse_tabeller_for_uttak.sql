create table if not exists UNG_UTTAK_PERIODER
(
    ID                          BIGINT                                      NOT NULL PRIMARY KEY,
    REGEL_INPUT                 OID                                         NOT NULL,
    REGEL_SPORING               OID                                         NOT NULL,
    OPPRETTET_AV                VARCHAR(20)     DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID               TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    ENDRET_AV                   VARCHAR(20)                                         ,
    ENDRET_TID                  TIMESTAMP(3)
);

create table if not exists UNG_UTTAK_PERIODE
(
    ID                          BIGINT                                      NOT NULL PRIMARY KEY,
    ung_uttak_perioder_id       BIGINT                                      not null,
    periode                     daterange                                   not null,
    utbetalingsgrad             numeric(19, 4)                              not null,
    OPPRETTET_AV                VARCHAR(20)     DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID               TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    ENDRET_AV                   VARCHAR(20)                                         ,
    ENDRET_TID                  TIMESTAMP(3)                                        ,

    constraint FK_UNG_GR_UNG_UTTAK_PERIODER foreign key (ung_uttak_perioder_id) references UNG_UTTAK_PERIODER,
    constraint UNG_UTTAK_PERIODE_IKKE_OVERLAPP EXCLUDE USING GIST (ung_uttak_perioder_id WITH =, periode WITH &&)
    );

alter table UNG_GR add column ung_uttak_perioder_id BIGINT;
alter table UNG_GR add constraint FK_UNG_GR_UNG_UTTAK_PERIODER foreign key (ung_uttak_perioder_id) references UNG_UTTAK_PERIODER;

create sequence if not exists SEQ_UNG_UTTAK_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNG_UTTAK_PERIODER increment by 50 minvalue 1000000;

create  index IDX_UNG_UTTAK_PERIODE_PERIODER on UNG_UTTAK_PERIODE (ung_uttak_perioder_id);
