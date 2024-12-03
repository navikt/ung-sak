/* Dropp gamle tabeller, indexer og sekvenser */
DROP INDEX IF EXISTS idx_gr_soeknadsperiode_oppgitt_soeknadperiode;
DROP INDEX IF EXISTS idx_ung_gr_soeknadsperiode_behandling;
DROP INDEX IF EXISTS idx_ung_soeknadsperiode_journalpost;
DROP INDEX IF EXISTS idx_ung_soeknadsperiode_perioder;
DROP INDEX IF EXISTS uidx_ung_gr_soeknadsperiode_aktiv_behandling;

DROP TABLE IF EXISTS ung_gr_soeknadsperiode CASCADE;
DROP TABLE IF EXISTS ung_soeknadsperiode CASCADE;
DROP TABLE IF EXISTS ung_soeknadsperioder CASCADE;

DROP SEQUENCE IF EXISTS seq_ung_gr_soeknadsperiode;
DROP SEQUENCE IF EXISTS seq_ung_soeknadsperiode;
DROP SEQUENCE IF EXISTS seq_ung_soeknadsperioder;
/* Dropp gamle tabeller, indexer og sekvenser */

create table if not exists UNG_SOEKNADER
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);
create table if not exists UNG_SOEKT_STARTDATO
(
    ID               BIGINT                                 NOT NULL PRIMARY KEY,
    JOURNALPOST_ID   VARCHAR(20)                            NOT NULL,
    UNG_SOEKNADER_ID BIGINT REFERENCES UNG_SOEKNADER (id),
    STARTDATO        DATE                                   NOT NULL,
    VERSJON          BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV     VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID    TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV        VARCHAR(20),
    ENDRET_TID       TIMESTAMP(3)
);
create index IDX_UNG_SOEKT_STARTDATO_JOURNALPOST on UNG_SOEKT_STARTDATO (JOURNALPOST_ID);
create index IDX_UNG_SOEKT_STARTDATO_PERIODER on UNG_SOEKT_STARTDATO (UNG_SOEKNADER_ID);
create sequence if not exists SEQ_UNG_SOEKNADER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNG_SOEKT_STARTDATO increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UNG_GR_SOEKNADGRUNNLAG increment by 50 minvalue 1000000;
create table UNG_GR_SOEKNADGRUNNLAG
(
    ID                     bigint                               NOT NULL PRIMARY KEY,
    BEHANDLING_ID          bigint REFERENCES BEHANDLING (id)    NOT NULL,
    RELEVANTE_SOEKNADER_ID bigint REFERENCES UNG_SOEKNADER (id),
    OPPGITTE_SOEKNADER_ID  bigint REFERENCES UNG_SOEKNADER (id) NOT NULL,
    VERSJON                bigint       default 0               NOT NULL,
    AKTIV                  boolean      default true            NOT NULL,
    OPPRETTET_AV           VARCHAR(20)  default 'VL'            NOT NULL,
    OPPRETTET_TID          TIMESTAMP(3) default localtimestamp  NOT NULL,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3)
);
create index IDX_UNG_GR_SOEKNADGRUNNLAG_BEHANDLING on UNG_GR_SOEKNADGRUNNLAG (BEHANDLING_ID);
create index IDX_GR_OPPGITT_SOEKNAD on UNG_GR_SOEKNADGRUNNLAG (OPPGITTE_SOEKNADER_ID);
create index IDX_GR_RELEVANT_SOEKNAD on UNG_GR_SOEKNADGRUNNLAG (RELEVANTE_SOEKNADER_ID);
CREATE UNIQUE INDEX UIDX_UNG_GR_SOEKNADGRUNNLAG_AKTIV_BEHANDLING ON UNG_GR_SOEKNADGRUNNLAG (BEHANDLING_ID) WHERE (AKTIV = TRUE);
