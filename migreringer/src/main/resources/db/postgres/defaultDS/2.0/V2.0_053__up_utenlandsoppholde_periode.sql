create table if not exists UP_UTENLANDSOPPHOLD_PERIODE
(
    id              BIGINT                      NOT NULL PRIMARY KEY,
    holder_id       BIGINT REFERENCES UP_SOEKNAD_PERIODER (id),
    fom             date                        NOT NULL,
    tom             date                        NOT NULL,
    aktiv           BOOLEAN                     NOT NULL,
    landkode        varchar(3)                  NOT NULL,
    aarsak          varchar(100)                ,
    versjon         bigint                      NOT NULL DEFAULT 0,
    opprettet_av    varchar(20)                 NOT NULL DEFAULT 'VL'::character varying,
    opprettet_tid   timestamp(3)                NOT NULL DEFAULT CURRENT_TIMESTAMP,
    endret_av       varchar(20)                 ,
    endret_tid      timestamp(3)
    );
create index IDX_UP_UTENLANDSOPHOLD_PERIODE_01 on UP_UTENLANDSOPPHOLD_PERIODE (HOLDER_ID);
create sequence if not exists SEQ_UP_UTENLANDSOPPHOLD_PERIODE increment by 50 minvalue 1000000;
