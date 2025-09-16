create table if not exists UTTALELSER
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
    );
create table if not exists UTTALELSE_V2
(
    ID                      BIGINT                                  NOT NULL PRIMARY KEY,
    GRUNNLAG_REF            BIGINT                                  NOT NULL,
    SVAR_JOURNALPOST_ID     VARCHAR(20)                             NOT NULL,
    UTTALELSER_ID BIGINT    REFERENCES                              UTTALELSER (id),
    ENDRING_TYPE            VARCHAR(50)                             NOT NULL,
    FOM                     DATE                                    NOT NULL,
    TOM                     DATE                                    NOT NULL,
    HAR_UTTALELSE           BOOLEAN                                 NOT NULL,
    BEGRUNNELSE             TEXT,
    VERSJON                 BIGINT       DEFAULT 0                  NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'               NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3)
    );
create index IDX_UTTALELSE_V2_ENDRING_TYPE on UTTALELSE_V2 (ENDRING_TYPE);
create index IDX_UTTALELSE_V2_UTTALELSER on UTTALELSE_V2 (UTTALELSER_ID);
create sequence if not exists SEQ_UTTALELSER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UTTALELSE_V2 increment by 50 minvalue 1000000;
create sequence if not exists SEQ_GR_UTTALELSE increment by 50 minvalue 1000000;
create table GR_UTTALELSE
(
    ID                     bigint                               NOT NULL PRIMARY KEY,
    BEHANDLING_ID          bigint REFERENCES BEHANDLING (id)    NOT NULL,
    UTTALELSER_ID          bigint REFERENCES UTTALELSER (id)    NOT NULL,
    VERSJON                bigint       default 0               NOT NULL,
    AKTIV                  boolean      default true            NOT NULL,
    OPPRETTET_AV           VARCHAR(20)  default 'VL'            NOT NULL,
    OPPRETTET_TID          TIMESTAMP(3) default localtimestamp  NOT NULL,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3)
);
create index IDX_GR_UTTALELSE_BEHANDLING on GR_UTTALELSE (BEHANDLING_ID);
create index IDX_GR_UTTALELSE_UTTALELSER on GR_UTTALELSE (UTTALELSER_ID);
CREATE UNIQUE INDEX UIDX_GR_UTTALELSE_AKTIV_BEHANDLING ON GR_UTTALELSE (BEHANDLING_ID) WHERE (AKTIV = TRUE);
