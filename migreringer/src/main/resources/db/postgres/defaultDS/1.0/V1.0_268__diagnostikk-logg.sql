create table if not exists DIAGNOSTIKK_FAGSAK_LOGG
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    FAGSAK_ID     BIGINT                                 NOT NULL REFERENCES FAGSAK (ID),
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create sequence if not exists SEQ_DIAGNOSTIKK_FAGSAK_LOGG increment by 50 minvalue 1000000;
create index on DIAGNOSTIKK_FAGSAK_LOGG (FAGSAK_ID);