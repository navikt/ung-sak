create table if not exists MD_PLEIETRENGENDE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    AKTOER_ID     VARCHAR(50)                            NOT NULL,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);
create sequence if not exists SEQ_MD_PLEIETRENGENDE increment by 50 minvalue 1000000;

ALTER TABLE gr_medisinsk
    ADD COLUMN pleietrengende_id bigint REFERENCES MD_PLEIETRENGENDE;

create index IDX_GR_MEDISINSK_4
    on gr_medisinsk (pleietrengende_id);
