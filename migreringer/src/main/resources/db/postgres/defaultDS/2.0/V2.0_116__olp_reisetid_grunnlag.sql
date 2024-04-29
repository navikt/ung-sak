alter table olp_vurdert_opplaering_periode drop column vurdert_reisetid_id;

drop table olp_vurdert_reisetid;

create table if not exists OLP_VURDERT_REISETID_HOLDER
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3)
);
create sequence if not exists SEQ_OLP_VURDERT_REISETID_HOLDER increment by 50 minvalue 1000000;

create table if not exists OLP_VURDERT_REISETID
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID               BIGINT                                 NOT NULL,
    FOM                     DATE                                   NOT NULL,
    TOM                     DATE                                   NOT NULL,
    GODKJENT                BOOLEAN                                NOT NULL,
    BEGRUNNELSE             VARCHAR(4000)                          NOT NULL,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3),
    constraint FK_OLP_VURDERT_REISETID_1 foreign key (HOLDER_ID) references OLP_VURDERT_REISETID_HOLDER
);


alter table gr_opplaering add column vurdert_reisetid_holder_id BIGINT;
alter table gr_opplaering add constraint fk_gr_opplaering_5 foreign key (vurdert_reisetid_holder_id) references OLP_VURDERT_REISETID_HOLDER;
