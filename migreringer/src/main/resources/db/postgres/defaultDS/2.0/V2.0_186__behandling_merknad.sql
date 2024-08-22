create table BEHANDLING_MERKNAD
(
    ID            bigint                              not null PRIMARY KEY,
    BEHANDLING_ID bigint                              not null,
    AKTIV         boolean      default true           not null,
    HASTESAK      boolean                             not null,
    FRITEKST      varchar(2000)                       null,
    VERSJON       bigint       default 0              not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    constraint FK_BEHANDLING_MERKNAD_1 foreign key (BEHANDLING_ID) references BEHANDLING
);

create index IDX_BEHANDLING_MERKNAD_1 on BEHANDLING_MERKNAD (BEHANDLING_ID);

create sequence if not exists SEQ_BEHANDLING_MERKNAD increment by 50 minvalue 1000000;
