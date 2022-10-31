create table if not exists OLP_VURDERT_OPPLAERING_PERIODE
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID               BIGINT                                 NOT NULL,
    FOM                     DATE                                   NOT NULL,
    TOM                     DATE                                   NOT NULL,
    BEGRUNNELSE             VARCHAR(4000)                                  ,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3),
    constraint FK_OLP_VURDERT_OPPLAERING_PERIODE_1 foreign key (HOLDER_ID) references OLP_VURDERT_OPPLAERING
);
create sequence if not exists SEQ_OLP_VURDERT_OPPLAERING_PERIODE increment by 50 minvalue 1000000;
create index IDX_OLP_VURDERT_OPPLAERING_PERIODE_1 on OLP_VURDERT_OPPLAERING_PERIODE (HOLDER_ID);

alter table OLP_VURDERT_OPPLAERING drop column FOM;
alter table OLP_VURDERT_OPPLAERING drop column TOM;
alter table OLP_VURDERT_OPPLAERING drop column institusjon;
alter table OLP_VURDERT_OPPLAERING add column journalpost_id varchar(50) not null;

create unique index UIDX_OLP_VURDERT_OPPLAERING_1 ON OLP_VURDERT_OPPLAERING (journalpost_id, holder_id);

