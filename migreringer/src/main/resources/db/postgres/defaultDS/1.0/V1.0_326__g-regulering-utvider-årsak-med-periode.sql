create table if not exists PT_TRIGGERE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists PT_TRIGGER
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    TRIGGERE_ID   BIGINT REFERENCES PT_TRIGGERE (id),
    arsak        varchar(100)                           not null,
    periode       daterange                              NOT NULL,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);
create index IDX_PT_TRIGGER_1
    on PT_TRIGGER (TRIGGERE_ID);

create table PROSESS_TRIGGERE
(
    ID            bigint                              not null PRIMARY KEY,
    BEHANDLING_ID bigint                              not null,
    triggere_id   bigint                              NOT NULL,
    VERSJON       bigint       default 0              not null,
    AKTIV         boolean      default true           not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    constraint FK_PROSESS_TRIGGERE_1
        foreign key (BEHANDLING_ID) references BEHANDLING,
    constraint FK_PROSESS_TRIGGERE_2
        foreign key (triggere_id) references PT_TRIGGERE
);

create sequence if not exists SEQ_PT_TRIGGER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_PT_TRIGGERE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_PROSESS_TRIGGERE increment by 50 minvalue 1000000;

create index IDX_PROSESS_TRIGGERE_1
    on PROSESS_TRIGGERE (BEHANDLING_ID);

create index IDX_PROSESS_TRIGGERE_2
    on PROSESS_TRIGGERE (triggere_id);

CREATE UNIQUE INDEX UIDX_PROSESS_TRIGGERE_01 ON PROSESS_TRIGGERE (BEHANDLING_ID) WHERE (AKTIV = TRUE);
