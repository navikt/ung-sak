create table if not exists DUMP_FEIL_IM_GR
(
    ID            bigint                              not null PRIMARY KEY,
    BEHANDLING_ID bigint                              not null,
    AKTIV         boolean      default true           not null,
    VERSJON       bigint       default 0              not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    constraint FK_DUMP_FEIL_IM_GR_01
        foreign key (BEHANDLING_ID) references behandling
);


CREATE TABLE if not exists DUMP_FEIL_IM_VILKAR_PERIODE
(
    ID                                  bigint                              not null PRIMARY KEY,
    dump_grunnlag_id                    bigint                              not null,
    fom                                 DATE                                not null,
    tom                                 DATE                                not null,
    VERSJON                                 bigint       default 0              not null,
    OPPRETTET_AV                        VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID                       TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV                           VARCHAR(20),
    ENDRET_TID                          TIMESTAMP(3),
    constraint FK_DUMP_FEIL_IM_VILKAR_PERIODE_01
        foreign key (dump_grunnlag_id) references DUMP_FEIL_IM_GR
);

CREATE TABLE if not exists DUMP_FEIL_IM_FORDEL_PERIODE
(
    ID                                  bigint                              not null PRIMARY KEY,
    dump_grunnlag_id                    bigint                              not null,
    fom                                 DATE                                not null,
    tom                                 DATE                                not null,
    VERSJON                                 bigint       default 0              not null,
    OPPRETTET_AV                        VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID                       TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV                           VARCHAR(20),
    ENDRET_TID                          TIMESTAMP(3),
    constraint FK_DUMP_FEIL_IM_FORDEL_PERIODE_01
    foreign key (dump_grunnlag_id) references DUMP_FEIL_IM_GR
    );

create sequence if not exists SEQ_DUMP_FEIL_IM_GR increment by 50 minvalue 1000000;
create sequence if not exists SEQ_DUMP_FEIL_IM_FORDEL_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_DUMP_FEIL_IM_VILKAR_PERIODE increment by 50 minvalue 1000000;
