create table ETTERLYSNING
(
    ID                    bigint                              not null PRIMARY KEY,
    fagsak_id             bigint references FAGSAK (id)       not null,
    behandling_id         bigint references BEHANDLING (id)   NOT NULL,
    ARBEIDSGIVER_AKTOR_ID VARCHAR(100),
    ARBEIDSGIVER_ORGNR    VARCHAR(100),
    FOM                   DATE                                NOT NULL,
    TOM                   DATE                                NOT NULL,
    dokument_mal          VARCHAR(100)                        NOT NULL,
    VERSJON               bigint       default 0              not null,
    OPPRETTET_AV          VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID         TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV             VARCHAR(20),
    ENDRET_TID            TIMESTAMP(3)
);

CREATE UNIQUE INDEX UIDX_ETTERLYSNING_01 ON ETTERLYSNING (fagsak_id);
CREATE UNIQUE INDEX UIDX_ETTERLYSNING_02 ON ETTERLYSNING (behandling_id);
CREATE UNIQUE INDEX UIDX_ETTERLYSNING_03 ON ETTERLYSNING (fagsak_id, behandling_id);
CREATE UNIQUE INDEX UIDX_ETTERLYSNING_04 ON ETTERLYSNING (fagsak_id, behandling_id, dokument_mal);

create sequence if not exists SEQ_ETTERLYSNING increment by 50 minvalue 1000000;
