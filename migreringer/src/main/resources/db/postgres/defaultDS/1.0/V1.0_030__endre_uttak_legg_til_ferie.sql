
alter sequence SEQ_UT_UTTAK rename to SEQ_UT_UTTAK_AKTIVITET;
alter sequence SEQ_UT_UTTAK_PERIODE rename to SEQ_UT_UTTAK_AKTIVITET_PERIODE;

alter table UT_UTTAK rename to UT_UTTAK_AKTIVITET;
alter table UT_UTTAK_PERIODE rename to UT_UTTAK_AKTIVITET_PERIODE;

alter table UT_UTTAK_AKTIVITET_PERIODE rename column "uttak_id" to "aktivitet_id";
alter table UT_UTTAK_AKTIVITET_PERIODE rename column "uttak_aktivitet_type" to "aktivitet_type";
alter table UT_UTTAK_AKTIVITET_PERIODE alter column aktivitet_id SET NOT NULL;
 
-- Legg til Ferie som nye tabeller
alter table GR_UTTAK 
  add column ferie_id bigint;
  
create table if not exists UT_FERIE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists UT_FERIE_PERIODE
(
    ID            				BIGINT                                 NOT NULL PRIMARY KEY,
    FERIE_ID  	  				BIGINT 								   NOT NULL REFERENCES UT_FERIE (id),
    FOM           				DATE                                   NOT NULL,
    TOM           				DATE                                   NOT NULL,
    VERSJON       BIGINT       	DEFAULT 0                 				NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  	DEFAULT 'VL'              				NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) 	DEFAULT CURRENT_TIMESTAMP 				NOT NULL,
    ENDRET_AV     				VARCHAR(20),
    ENDRET_TID    				TIMESTAMP(3)
);

create sequence if not exists SEQ_UT_FERIE_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UT_FERIE increment by 50 minvalue 1000000;

create index IDX_UT_FERIE_PERIODE_01 on UT_FERIE_PERIODE (FERIE_ID);

create index IDX_GR_UTTAK_03 on GR_UTTAK (ferie_id);

alter table GR_UTTAK add constraint FK_GR_UTTAK_05 FOREIGN KEY (ferie_id) REFERENCES UT_FERIE(id);

