create table if not exists MD_OMSORGENFOR
(
    ID             BIGINT                                 NOT NULL PRIMARY KEY,
    har_omsorg_for boolean                                NOT NULL,
    VERSJON        BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV   VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV      VARCHAR(20),
    ENDRET_TID     TIMESTAMP(3)
);
create sequence if not exists SEQ_MD_OMSORGENFOR increment by 50 minvalue 1000000;

alter table GR_MEDISINSK
    add column omsorgenfor_id bigint REFERENCES MD_OMSORGENFOR;

create index IDX_GR_MEDISINSK_5
    on gr_medisinsk (pleietrengende_id);
