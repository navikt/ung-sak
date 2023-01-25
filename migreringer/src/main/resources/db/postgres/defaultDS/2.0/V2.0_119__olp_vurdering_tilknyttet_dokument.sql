create table if not exists OPPLAERING_DOKUMENT
(
    ID                          BIGINT                                      NOT NULL PRIMARY KEY,
    JOURNALPOST_ID              VARCHAR(50)                                 NOT NULL,
    DOKUMENT_INFO_ID            VARCHAR(50)                                 ,
    SOEKERS_BEHANDLING_UUID     UUID                                        ,
    SOEKERS_SAKSNUMMER          VARCHAR(19)                                 NOT NULL,
    SOEKERS_PERSON_ID           BIGINT                                      ,
    OPPRETTET_AV                VARCHAR(20)  DEFAULT 'VL'                   NOT NULL,
    OPPRETTET_TID               TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP      NOT NULL
);
create sequence if not exists SEQ_OPPLAERING_DOKUMENT increment by 50 minvalue 1000000;
create unique index UIDX_OPPLAERING_DOKUMENT_1 ON OPPLAERING_DOKUMENT (JOURNALPOST_ID, DOKUMENT_INFO_ID);

create table if not exists OPPLAERING_DOKUMENT_INFORMASJON
(
    ID                                  BIGINT                                      NOT NULL PRIMARY KEY,
    OPPLAERING_DOKUMENT_ID              BIGINT                                      ,
    DUPLIKAT_AV_OPPLAERING_DOKUMENT_ID  BIGINT                                      ,
    VERSJON                             BIGINT                                      NOT NULL,
    TYPE                                VARCHAR(20)                                 NOT NULL,
    DATERT                              DATE                                        ,
    MOTTATT                             TIMESTAMP(3)                                NOT NULL,
    OPPRETTET_AV                        VARCHAR(20)  DEFAULT 'VL'                   NOT NULL,
    OPPRETTET_TID                       TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP      NOT NULL,
    har_info_som_ikke_kan_punsjes       BOOLEAN      DEFAULT FALSE                  NOT NULL,
    CONSTRAINT FK_OPPLAERING_DOKUMENT_INFORMASJON_1 FOREIGN KEY (OPPLAERING_DOKUMENT_ID) REFERENCES OPPLAERING_DOKUMENT(ID),
    CONSTRAINT FK_OPPLAERING_DOKUMENT_INFORMASJON_2 FOREIGN KEY (DUPLIKAT_AV_OPPLAERING_DOKUMENT_ID) REFERENCES OPPLAERING_DOKUMENT(ID)

);
create sequence if not exists SEQ_OPPLAERING_DOKUMENT_INFORMASJON increment by 50 minvalue 1000000;
create unique index UIDX_OPPLAERING_DOKUMENT_INFORMASJON_1 ON OPPLAERING_DOKUMENT_INFORMASJON (OPPLAERING_DOKUMENT_ID, VERSJON);

create table if not exists OLP_VURDERT_OPPLAERING_PERIODE_ANVENDT_DOKUMENT
(
    OPPLAERING_DOKUMENT_ID          BIGINT                                      NOT NULL,
    VURDERT_OPPLAERING_PERIODE_ID   BIGINT                                      NOT NULL,
    OPPRETTET_AV                    VARCHAR(20)     DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID                   TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    CONSTRAINT FK_OLP_VURDERT_OPPLAERING_PERIODE_ANVENDT_DOKUMENT_1 FOREIGN KEY (OPPLAERING_DOKUMENT_ID) REFERENCES OPPLAERING_DOKUMENT(ID),
    CONSTRAINT FK_OLP_VURDERT_OPPLAERING_PERIODE_ANVENDT_DOKUMENT_2 FOREIGN KEY (VURDERT_OPPLAERING_PERIODE_ID) REFERENCES olp_vurdert_opplaering_periode(ID)
);
create unique index UIDX_OLP_VURDERT_OPPLAERING_PERIODE_ANVENDT_DOKUMENT_1 ON OLP_VURDERT_OPPLAERING_PERIODE_ANVENDT_DOKUMENT (OPPLAERING_DOKUMENT_ID, VURDERT_OPPLAERING_PERIODE_ID);

create table if not exists OLP_VURDERT_OPPLAERING_ANVENDT_DOKUMENT
(
    OPPLAERING_DOKUMENT_ID          BIGINT                                      NOT NULL,
    VURDERT_OPPLAERING_ID           BIGINT                                      NOT NULL,
    OPPRETTET_AV                    VARCHAR(20)     DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID                   TIMESTAMP(3)    DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    CONSTRAINT FK_OLP_VURDERT_OPPLAERING_ANVENDT_DOKUMENT_1 FOREIGN KEY (OPPLAERING_DOKUMENT_ID) REFERENCES OPPLAERING_DOKUMENT(ID),
    CONSTRAINT FK_OLP_VURDERT_OPPLAERING_ANVENDT_DOKUMENT_2 FOREIGN KEY (VURDERT_OPPLAERING_ID) REFERENCES olp_vurdert_opplaering(ID)
);
create unique index UIDX_OLP_VURDERT_OPPLAERING_ANVENDT_DOKUMENT_1 ON OLP_VURDERT_OPPLAERING_ANVENDT_DOKUMENT (OPPLAERING_DOKUMENT_ID, VURDERT_OPPLAERING_ID);
