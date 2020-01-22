create table if not exists VILKAR_PERIODE
(
    ID                 BIGINT                                 NOT NULL PRIMARY KEY,
    VILKAR_ID          BIGINT REFERENCES VILKAR (id),
    FOM                DATE                                   NOT NULL,
    TOM                DATE                                   NOT NULL,
    manuelt_vurdert    BOOLEAN                                NOT NULL,
    UTFALL             VARCHAR(100)                           NOT NULL,
    MERKNAD            VARCHAR(100)                           NOT NULL,
    OVERSTYRT_UTFALL   VARCHAR(100) DEFAULT '-'               NOT NULL,
    AVSLAG_KODE        VARCHAR(100),
    MERKNAD_PARAMETERE VARCHAR(1000),
    REGEL_EVALUERING   TEXT,
    REGEL_INPUT        TEXT,
    VERSJON            BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV       VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID      TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV          VARCHAR(20),
    ENDRET_TID         TIMESTAMP(3)
);

create sequence if not exists SEQ_VILKAR_PERIODE increment by 50 minvalue 1000000;

ALTER TABLE vilkar
    DROP column vilkar_utfall;
ALTER TABLE vilkar
    DROP column vilkar_utfall_overstyrt;
ALTER TABLE vilkar
    DROP column vilkar_utfall_manuell;
ALTER TABLE vilkar
    DROP column vilkar_utfall_merknad;
ALTER TABLE vilkar
    DROP column merknad_parametere;
ALTER TABLE vilkar
    DROP column regel_evaluering;
ALTER TABLE vilkar
    DROP column regel_input;
ALTER TABLE vilkar
    DROP column AVSLAG_KODE;

alter table vilkar_resultat
    drop column original_behandling_id;
