create table if not exists UP_UTTAKSPERIODER_HOLDER
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists UP_SOEKNAD_PERIODER
(
    ID             BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID      BIGINT REFERENCES UP_UTTAKSPERIODER_HOLDER (id),
    JOURNALPOST_ID VARCHAR(20)                            not null,
    VERSJON        BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV   VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV      VARCHAR(20),
    ENDRET_TID     TIMESTAMP(3)
);

create index IDX_UP_SOEKNAD_PERIODER_01
    on UP_SOEKNAD_PERIODER (HOLDER_ID);
create index IDX_UP_SOEKNAD_PERIODER_02
    on UP_SOEKNAD_PERIODER (JOURNALPOST_ID);

create table if not exists UP_UTTAKSPERIODE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID     BIGINT REFERENCES UP_SOEKNAD_PERIODER (id),
    FOM           DATE                                   NOT NULL,
    TOM           DATE                                   NOT NULL,
    TIMER_PER_DAG VARCHAR(20)                            NOT NULL,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);
create index IDX_UP_UTTAKSPERIODE_01
    on UP_UTTAKSPERIODE (HOLDER_ID);

create table if not exists UP_ARBEID_PERIODE
(
    ID                       BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID                BIGINT REFERENCES UP_SOEKNAD_PERIODER (id),
    FOM                      DATE                                   NOT NULL,
    TOM                      DATE                                   NOT NULL,
    AKTIVITET_TYPE           VARCHAR(100)                           NOT NULL,
    ARBEIDSGIVER_AKTOR_ID    VARCHAR(100),
    ARBEIDSGIVER_ORGNR       VARCHAR(100),
    ARBEIDSFORHOLD_INTERN_ID UUID,
    NORMALT_ARBEID_PER_DAG   VARCHAR(20)                            NOT NULL,
    FAKTISK_ARBEID_PER_DAG   VARCHAR(20)                            NOT NULL,
    VERSJON                  BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV             VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID            TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV                VARCHAR(20),
    ENDRET_TID               TIMESTAMP(3)
);
create index IDX_UP_ARBEID_PERIODE_01
    on UP_ARBEID_PERIODE (HOLDER_ID);

create table if not exists UP_TILSYNSORDNING
(
    ID                  BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID           BIGINT REFERENCES UP_SOEKNAD_PERIODER (id),
    TILSYNSORDNING_SVAR VARCHAR(10),
    VERSJON             BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV        VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID       TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV           VARCHAR(20),
    ENDRET_TID          TIMESTAMP(3)
);

create table if not exists UP_TILSYNSORDNING_PERIODE
(
    ID                BIGINT                                 NOT NULL PRIMARY KEY,
    TILSYNSORDNING_ID BIGINT                                 NOT NULL REFERENCES UP_TILSYNSORDNING (id),
    FOM               DATE                                   NOT NULL,
    TOM               DATE                                   NOT NULL,
    VARIGHET          VARCHAR(20)                            NOT NULL, -- angir varighet(duratin) ISO8601 std. eks. PT30M
    VERSJON           BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV      VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID     TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV         VARCHAR(20),
    ENDRET_TID        TIMESTAMP(3)
);

create index IDX_UP_TILSYNSORDNING_01 on UP_TILSYNSORDNING (HOLDER_ID);
create index IDX_UP_TILSYNSORDNING_PERIODE_01 on UP_TILSYNSORDNING_PERIODE (TILSYNSORDNING_ID);

create table if not exists UP_FERIE_PERIODE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID     BIGINT REFERENCES UP_SOEKNAD_PERIODER (id),
    FOM           DATE                                   NOT NULL,
    TOM           DATE                                   NOT NULL,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);
create index IDX_UP_FERIE_PERIODE_01 on UP_FERIE_PERIODE (HOLDER_ID);

create sequence if not exists SEQ_UP_ARBEID_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UP_FERIE_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UP_TILSYNSORDNING_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UP_TILSYNSORDNING increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UP_UTTAKSPERIODER_HOLDER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UP_SOEKNAD_PERIODER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UP_UTTAKSPERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_GR_UTTAKSPERIODER increment by 50 minvalue 1000000;

create table GR_UTTAKSPERIODER
(
    ID                         bigint                                          not null PRIMARY KEY,
    BEHANDLING_ID              bigint REFERENCES BEHANDLING (id)               not null,
    RELEVANT_SOKNADSPERIODE_ID bigint REFERENCES UP_UTTAKSPERIODER_HOLDER (id),
    OPPGITT_SOKNADSPERIODE_ID  bigint REFERENCES UP_UTTAKSPERIODER_HOLDER (id) not null,
    VERSJON                    bigint       default 0                          not null,
    AKTIV                      boolean      default true                       not null,
    OPPRETTET_AV               VARCHAR(20)  default 'VL'                       not null,
    OPPRETTET_TID              TIMESTAMP(3) default localtimestamp             not null,
    ENDRET_AV                  VARCHAR(20),
    ENDRET_TID                 TIMESTAMP(3)
);


create index IDX_GR_UTTAKSPERIODER_01
    on GR_UTTAKSPERIODER (BEHANDLING_ID);
create index IDX_GR_UTTAKSPERIODER_02
    on GR_UTTAKSPERIODER (oppgitt_SOKNADSPERIODE_id);

CREATE UNIQUE INDEX UIDX_GR_UTTAKSPERIODER_01 ON GR_UTTAKSPERIODER (BEHANDLING_ID) WHERE (AKTIV = TRUE);
