create table if not exists UTTAK_NYE_REGLER
(
    ID            BIGINT                                 NOT NULL,
    BEHANDLING_ID BIGINT                                 NOT NULL,
    VIRKNINGSDATO DATE                                   NOT NULL,
    AKTIV         BOOLEAN      DEFAULT false             NOT NULL,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

alter table UTTAK_NYE_REGLER add constraint FK_UTTAK_NYE_REGLER_01 foreign key (BEHANDLING_ID) references BEHANDLING(ID);
create UNIQUE index UIDX_UTTAK_NYE_REGLER_1 on UTTAK_NYE_REGLER (BEHANDLING_ID) WHERE AKTIV;

create sequence if not exists seq_uttak_nye_regler increment by 50 minvalue 1000000;
