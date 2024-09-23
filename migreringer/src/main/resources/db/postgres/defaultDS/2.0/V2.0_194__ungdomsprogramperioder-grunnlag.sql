create table if not exists UNG_UNGDOMSPROGRAMPERIODER
(
    ID             BIGINT                                 NOT NULL PRIMARY KEY,
    OPPRETTET_AV   VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV      VARCHAR(20),
    ENDRET_TID     TIMESTAMP(3)
);

create table if not exists UNG_UNGDOMSPROGRAMPERIODE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    UNG_UNGDOMSPROGRAMPERIODER_ID   BIGINT REFERENCES UNG_UNGDOMSPROGRAMPERIODER (id),
    FOM           DATE                                   NOT NULL,
    TOM           DATE                                   NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create sequence if not exists SEQ_UNG_UNGDOMSPROGRAMPERIODER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNG_UNGDOMSPROGRAMPERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNG_GR_UNGDOMSPROGRAMPERIODE increment by 50 minvalue 1000000;

create index IDX_UNG_UNGDOMSPROGRAMPERIODE_PERIODER
    on UNG_UNGDOMSPROGRAMPERIODE (UNG_UNGDOMSPROGRAMPERIODER_ID);

create table UNG_GR_UNGDOMSPROGRAMPERIODE
(
    ID                        bigint                                            not null PRIMARY KEY,
    BEHANDLING_ID             bigint REFERENCES BEHANDLING (id)                 not null,
    UNG_UNGDOMSPROGRAMPERIODER_ID bigint REFERENCES UNG_UNGDOMSPROGRAMPERIODER (id) not null,
    AKTIV                     boolean      default true                         not null,
    OPPRETTET_AV              VARCHAR(20)  default 'VL'                         not null,
    OPPRETTET_TID             TIMESTAMP(3) default localtimestamp               not null,
    ENDRET_AV                 VARCHAR(20),
    ENDRET_TID                TIMESTAMP(3)
);

create index IDX_UNG_GR_UNGDOMSPROGRAMPERIODE_BEHANDLING
    on UNG_GR_UNGDOMSPROGRAMPERIODE (BEHANDLING_ID);
create index IDX_UNG_GR_UNGDOMSPROGRAMPERIODE_PERIODER
    on UNG_GR_UNGDOMSPROGRAMPERIODE (UNG_UNGDOMSPROGRAMPERIODER_ID);

CREATE UNIQUE INDEX UIDX_UNG_GR_UNGDOMSPROGRAMPERIODE_AKTIV_BEHANDLING ON UNG_GR_UNGDOMSPROGRAMPERIODE (BEHANDLING_ID) WHERE (AKTIV = TRUE);
