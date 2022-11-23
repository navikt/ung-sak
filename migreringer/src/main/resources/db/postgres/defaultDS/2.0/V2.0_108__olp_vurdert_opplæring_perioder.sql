alter table UP_KURS_PERIODE rename column reiseTilFom to REISE_TIL_FOM;
alter table UP_KURS_PERIODE rename column reiseTilTom to REISE_TIL_TOM;
alter table UP_KURS_PERIODE rename column reiseHjemFom to REISE_HJEM_FOM;
alter table UP_KURS_PERIODE rename column reiseHjemTom to REISE_HJEM_TOM;

create table if not exists OLP_VURDERT_OPPLAERING_PERIODER_HOLDER
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3)
);
create sequence if not exists SEQ_OLP_VURDERT_OPPLAERING_PERIODER_HOLDER increment by 50 minvalue 1000000;

create table if not exists OLP_VURDERT_REISETID
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    REISE_TIL_FOM           DATE                                           ,
    REISE_TIL_TOM           DATE                                           ,
    REISE_HJEM_FOM          DATE                                           ,
    REISE_HJEM_TOM          DATE                                           ,
    BEGRUNNELSE             VARCHAR(4000)                          NOT NULL,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3)
);
create sequence if not exists SEQ_OLP_VURDERT_REISETID increment by 50 minvalue 1000000;

create table if not exists OLP_VURDERT_OPPLAERING_PERIODE
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    HOLDER_ID               BIGINT                                 NOT NULL,
    FOM                     DATE                                   NOT NULL,
    TOM                     DATE                                   NOT NULL,
    GJENNOMFOERT_OPPLAERING BOOLEAN                                NOT NULL,
    VURDERT_REISETID_ID     BIGINT                                         ,
    BEGRUNNELSE             VARCHAR(4000)                          NOT NULL,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3),
    constraint FK_OLP_VURDERT_OPPLAERING_PERIODE_1 foreign key (HOLDER_ID) references OLP_VURDERT_OPPLAERING_PERIODER_HOLDER,
    constraint FK_OLP_VURDERT_OPPLAERING_PERIODE_2 foreign key (VURDERT_REISETID_ID) references OLP_VURDERT_REISETID,
    CONSTRAINT OLP_VURDERT_OPPLAERING_PERIODE_IKKE_OVERLAPP EXCLUDE USING GIST (
        HOLDER_ID WITH =,
        TSRANGE(FOM, TOM) WITH &&
        )
);
create sequence if not exists SEQ_OLP_VURDERT_OPPLAERING_PERIODE increment by 50 minvalue 1000000;
create index IDX_OLP_VURDERT_OPPLAERING_PERIODE_1 on OLP_VURDERT_OPPLAERING_PERIODE (HOLDER_ID);

alter table GR_OPPLAERING add column vurderte_perioder_id BIGINT;
alter table GR_OPPLAERING add constraint FK_GR_OPPLAERING_4 foreign key (vurderte_perioder_id) references OLP_VURDERT_OPPLAERING_PERIODER_HOLDER;
create index IDX_GR_OPPLAERING_4 on GR_OPPLAERING (vurderte_perioder_id);

alter table olp_vurdert_opplaering alter column begrunnelse set not null;
alter table olp_vurdert_institusjon alter column begrunnelse set not null;
