create table if not exists SP_SOEKNADSPERIODER_HOLDER
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists SP_SOEKNADSPERIODER
(
    ID             BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID      BIGINT REFERENCES SP_SOEKNADSPERIODER_HOLDER (id),
    JOURNALPOST_ID VARCHAR(20)                            not null,
    VERSJON        BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV   VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV      VARCHAR(20),
    ENDRET_TID     TIMESTAMP(3)
);

create index IDX_SP_SOEKNADSPERIODER_01
    on SP_SOEKNADSPERIODER (HOLDER_ID);
create index IDX_SP_SOEKNADSPERIODER_02
    on SP_SOEKNADSPERIODER (JOURNALPOST_ID);

create table if not exists SP_SOEKNADSPERIODE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID   BIGINT REFERENCES SP_SOEKNADSPERIODER (id),
    FOM           DATE                                   NOT NULL,
    TOM           DATE                                   NOT NULL,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create sequence if not exists SEQ_SP_SOEKNADSPERIODER_HOLDER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_SP_SOEKNADSPERIODER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_SP_SOEKNADSPERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_GR_SOEKNADSPERIODE increment by 50 minvalue 1000000;

create index IDX_SP_SOEKNADSPERIODE_01
    on SP_SOEKNADSPERIODE (HOLDER_ID);

create table GR_SOEKNADSPERIODE
(
    ID                        bigint                                            not null PRIMARY KEY,
    BEHANDLING_ID             bigint REFERENCES BEHANDLING (id)                 not null,
    RELEVANT_SOKNADSPERIODE_ID bigint REFERENCES SP_SOEKNADSPERIODER_HOLDER (id) not null,
    OPPGITT_SOKNADSPERIODE_ID bigint REFERENCES SP_SOEKNADSPERIODER_HOLDER (id) not null,
    VERSJON                   bigint       default 0                            not null,
    AKTIV                     boolean      default true                         not null,
    OPPRETTET_AV              VARCHAR(20)  default 'VL'                         not null,
    OPPRETTET_TID             TIMESTAMP(3) default localtimestamp               not null,
    ENDRET_AV                 VARCHAR(20),
    ENDRET_TID                TIMESTAMP(3)
);

create index IDX_GR_SOEKNADSPERIODE_01
    on GR_SOEKNADSPERIODE (BEHANDLING_ID);
create index IDX_GR_SOEKNADSPERIODE_02
    on GR_SOEKNADSPERIODE (oppgitt_SOKNADSPERIODE_id);

CREATE UNIQUE INDEX UIDX_GR_SOEKNADSPERIODE_01 ON GR_SOEKNADSPERIODE (BEHANDLING_ID) WHERE (AKTIV = TRUE);
