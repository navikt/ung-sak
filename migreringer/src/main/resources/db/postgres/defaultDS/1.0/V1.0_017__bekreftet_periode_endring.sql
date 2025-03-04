create table if not exists UNG_BEKREFTET_PERIODE_ENDRING
(
    ID                      BIGINT                                          NOT NULL PRIMARY KEY,
    JOURNALPOST_ID          VARCHAR(20)                                     NOT NULL,
    UNG_GR_STARTDATO_ID     BIGINT REFERENCES UNG_GR_STARTDATO (id)         NOT NULL,
    DATO                    DATE                                            NOT NULL,
    ENDRING_TYPE            VARCHAR(100)                                    NOT NULL,
    VERSJON                 BIGINT       DEFAULT 0                          NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'                       NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP          NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3)
    );


create index idx_ung_bekreftet_periode_endring_gr_id
    on UNG_BEKREFTET_PERIODE_ENDRING (UNG_GR_STARTDATO_ID);

create unique index idx_ung_bekreftet_periode_endring_jp_id
    on UNG_BEKREFTET_PERIODE_ENDRING (JOURNALPOST_ID);

create sequence if not exists seq_ung_bekreftet_periode_endring increment by 50 minvalue 1000000;


comment on table UNG_BEKREFTET_PERIODE_ENDRING is 'Endringer i ungdomsprogrammet som er bekreftet av søker.';
comment on column UNG_BEKREFTET_PERIODE_ENDRING.ID is 'Primary Key. Unik identifikator.';
comment on column UNG_BEKREFTET_PERIODE_ENDRING.UNG_GR_STARTDATO_ID is 'Referanse til grunnlag.';
comment on column UNG_BEKREFTET_PERIODE_ENDRING.OPPRETTET_TID is 'Tidspunkt for når bekreftelsen ble opprettet.';
