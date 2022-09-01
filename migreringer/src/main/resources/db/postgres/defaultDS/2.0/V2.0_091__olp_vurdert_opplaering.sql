create table if not exists OLP_VURDERT_OPPLAERING_GRUNNLAG
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    BEHANDLING_ID           BIGINT                                 NOT NULL,
    GODKJENT_INSTITUSJON    BOOLEAN      DEFAULT false             NOT NULL,
    AKTIV                   BOOLEAN      DEFAULT false             NOT NULL,
    BEGRUNNELSE             VARCHAR(4000)                                  ,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3),
    constraint FK_OLP_VURDERT_OPPLAERING_GRUNNLAG_1
        foreign key (BEHANDLING_ID) references BEHANDLING
);
create sequence if not exists SEQ_OLP_VURDERT_OPPLAERING_GRUNNLAG increment by 50 minvalue 1000000;

create table if not exists OLP_VURDERT_OPPLAERING
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    OLP_VURDERT_OPPLAERING_GRUNNLAG_ID BIGINT                      NOT NULL,
    FOM                     DATE                                   NOT NULL,
    TOM                     DATE                                   NOT NULL,
    NOEDVENDIG_OPPLAERING   BOOLEAN      DEFAULT false             NOT NULL,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3),
    constraint FK_OLP_VURDERT_OPPLAERING_1
        foreign key (OLP_VURDERT_OPPLAERING_GRUNNLAG_ID) references OLP_VURDERT_OPPLAERING_GRUNNLAG
);
create sequence if not exists SEQ_OLP_VURDERT_OPPLAERING increment by 50 minvalue 1000000;
