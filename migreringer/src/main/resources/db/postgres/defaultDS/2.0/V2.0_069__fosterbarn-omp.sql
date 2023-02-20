create sequence SEQ_OMP_FOSTERBARN increment by 50 minvalue 1000000;
create sequence SEQ_OMP_FOSTERBARNA increment by 50 minvalue 1000000;
create sequence SEQ_OMP_GR_FOSTERBARN increment by 50 minvalue 1000000;

create table OMP_FOSTERBARNA (
                                 ID                  BIGINT                                 NOT NULL PRIMARY KEY,
                                 VERSJON             BIGINT       DEFAULT 0                 NOT NULL,
                                 OPPRETTET_AV        VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
                                 OPPRETTET_TID       TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 ENDRET_AV           VARCHAR(20),
                                 ENDRET_TID          TIMESTAMP(3)
);

create table OMP_FOSTERBARN (
                                ID                  BIGINT                                 NOT NULL PRIMARY KEY,
                                FOSTERBARNA_ID      BIGINT REFERENCES OMP_FOSTERBARN(ID)   NOT NULL,
                                AKTOER_ID           VARCHAR(50)                            NOT NULL,
                                OPPRETTET_AV        VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
                                OPPRETTET_TID       TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                ENDRET_AV           VARCHAR(20),
                                ENDRET_TID          TIMESTAMP(3),
                                constraint FK_OMP_FOSTERBARN_1 foreign key (FOSTERBARNA_ID) references OMP_FOSTERBARNA
);

create table OMP_GR_FOSTERBARN (
                                   ID                               BIGINT                                 NOT NULL PRIMARY KEY,
                                   BEHANDLING_ID                    BIGINT                                 NOT NULL,
                                   OMP_FOSTERBARNA_ID               BIGINT,
                                   AKTIV                            BOOLEAN      DEFAULT TRUE              NOT NULL,
                                   VERSJON                          BIGINT       DEFAULT 0                 NOT NULL,
                                   OPPRETTET_AV                     VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
                                   OPPRETTET_TID                    TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                   ENDRET_AV                        VARCHAR(20),
                                   ENDRET_TID                       TIMESTAMP(3),
                                   constraint FK_OMP_GR_FOSTERBARN_1 foreign key (OMP_FOSTERBARNA_ID) references OMP_FOSTERBARNA
);
create index IDX_OMP_GR_FOSTERBARN_1 on OMP_GR_FOSTERBARN (BEHANDLING_ID);
create index IDX_OMP_GR_FOSTERBARN_2 on OMP_GR_FOSTERBARN (OMP_FOSTERBARNA_ID);

create unique index UIDX_OMP_GR_FOSTERBARN_1 on OMP_GR_FOSTERBARN (BEHANDLING_ID) where (aktiv = true);
create unique index UIDX_OMP_GR_FOSTERBARN_2 on OMP_GR_FOSTERBARN (OMP_FOSTERBARNA_ID) where (aktiv = true);
