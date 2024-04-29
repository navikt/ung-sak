create table if not exists DUMP_SIMULERT_UTB
(
    ID            bigint                              not null PRIMARY KEY,
    BEHANDLING_ID bigint                              not null,
    VERSJON       bigint       default 0              not null,
    AKTIV         boolean      default true           not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    constraint FK_DUMP_SIMULERT_UTB_01
        foreign key (BEHANDLING_ID) references behandling
);

CREATE TABLE if not exists DUMP_SIMULERT_UTB_DIFF
(
    ID                      bigint                              not null PRIMARY KEY,
    ekstern_referanse       uuid                              not null,
    kalkulus_request        JSONB                               NOT NULL,
    total_feilutbetaling_bruker           BIGINT                              not null,
    total_feilutbetaling_arbeidsgiver     BIGINT                              not null,
    dump_grunnlag_id        bigint                              not null,
    VERSJON                 bigint       default 0              not null,
    OPPRETTET_AV            VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID           TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3),
    constraint FK_DUMP_SIMULERT_UTB_DIFF_01
        foreign key (dump_grunnlag_id) references DUMP_SIMULERT_UTB
);

CREATE TABLE if not exists DUMP_SIMULERT_UTB_DIFF_PERIODE
(
    ID                                  bigint                              not null PRIMARY KEY,
    dump_simulert_utb_diff_id           bigint                              not null,
    fom                                 DATE                                not null,
    tom                                 DATE                                not null,
    total_feilutbetaling_bruker           BIGINT                              not null,
    total_feilutbetaling_arbeidsgiver     BIGINT                              not null,
    VERSJON                                 bigint       default 0              not null,
    OPPRETTET_AV                        VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID                       TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV                           VARCHAR(20),
    ENDRET_TID                          TIMESTAMP(3),
    constraint FK_DUMP_SIMULERT_UTB_DIFF_PERIODE_01
        foreign key (dump_simulert_utb_diff_id) references DUMP_SIMULERT_UTB_DIFF
);

CREATE TABLE if not exists DUMP_SIMULERT_UTB_DIFF_ANDEL
(
    ID                              bigint                              not null PRIMARY KEY,
    PERIODE_ID                      bigint,
    ARBEIDSGIVER_AKTOR_ID           VARCHAR(100),
    ARBEIDSGIVER_ORGNR              VARCHAR(100),
    dagsats_aktiv                   BIGINT                              not null,
    dagsats_simulert                BIGINT                              not null,
    dagsats_bruker_aktiv            BIGINT                              not null,
    dagsats_bruker_simulert         BIGINT                              not null,
    dagsats_arbeidsgiver_aktiv      BIGINT                              not null,
    dagsats_arbeidsgiver_simulert   BIGINT                              not null,
    VERSJON                         bigint       default 0              not null,
    OPPRETTET_AV                    VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID                   TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV                       VARCHAR(20),
    ENDRET_TID                      TIMESTAMP(3),
    constraint FK_DUMP_SIMULERT_UTB_DIFF_ANDEL_01
        foreign key (PERIODE_ID) references DUMP_SIMULERT_UTB_DIFF_PERIODE
);

create sequence if not exists SEQ_DUMP_SIMULERT_UTB increment by 50 minvalue 1000000;
create sequence if not exists SEQ_DUMP_SIMULERT_UTB_DIFF increment by 50 minvalue 1000000;
create sequence if not exists SEQ_DUMP_SIMULERT_UTB_DIFF_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_DUMP_SIMULERT_UTB_DIFF_ANDEL increment by 50 minvalue 1000000;
