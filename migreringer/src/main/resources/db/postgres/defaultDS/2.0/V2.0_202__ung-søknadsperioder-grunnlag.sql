create table if not exists UNG_SOEKNADSPERIODER
(
    ID             BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON        BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV   VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV      VARCHAR(20),
    ENDRET_TID     TIMESTAMP(3)
);




create table if not exists UNG_SOEKNADSPERIODE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    JOURNALPOST_ID VARCHAR(20)                            not null,
    UNG_SOEKNADSPERIODER_ID   BIGINT REFERENCES UNG_SOEKNADSPERIODER (id),
    FOM           DATE                                   NOT NULL,
    TOM           DATE                                   NOT NULL,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create index IDX_UNG_SOEKNADSPERIODE_JOURNALPOST
    on UNG_SOEKNADSPERIODE (JOURNALPOST_ID);

create index IDX_UNG_SOEKNADSPERIODE_PERIODER
    on UNG_SOEKNADSPERIODE (UNG_SOEKNADSPERIODER_ID);

create sequence if not exists SEQ_UNG_SOEKNADSPERIODER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNG_SOEKNADSPERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNG_GR_SOEKNADSPERIODE increment by 50 minvalue 1000000;

create table UNG_GR_SOEKNADSPERIODE
(
    ID                        bigint                                            not null PRIMARY KEY,
    BEHANDLING_ID             bigint REFERENCES BEHANDLING (id)                 not null,
    RELEVANT_SOKNADSPERIODE_ID bigint REFERENCES UNG_SOEKNADSPERIODER (id),
    OPPGITT_SOKNADSPERIODE_ID bigint REFERENCES UNG_SOEKNADSPERIODER (id) not null,
    VERSJON                   bigint       default 0                            not null,
    AKTIV                     boolean      default true                         not null,
    OPPRETTET_AV              VARCHAR(20)  default 'VL'                         not null,
    OPPRETTET_TID             TIMESTAMP(3) default localtimestamp               not null,
    ENDRET_AV                 VARCHAR(20),
    ENDRET_TID                TIMESTAMP(3)
);

create index IDX_UNG_GR_SOEKNADSPERIODE_BEHANDLING
    on UNG_GR_SOEKNADSPERIODE (BEHANDLING_ID);
create index IDX_GR_SOEKNADSPERIODE_OPPGITT_SOEKNADPERIODE
    on UNG_GR_SOEKNADSPERIODE (OPPGITT_SOKNADSPERIODE_ID);

CREATE UNIQUE INDEX UIDX_UNG_GR_SOEKNADSPERIODE_AKTIV_BEHANDLING ON UNG_GR_SOEKNADSPERIODE (BEHANDLING_ID) WHERE (AKTIV = TRUE);
