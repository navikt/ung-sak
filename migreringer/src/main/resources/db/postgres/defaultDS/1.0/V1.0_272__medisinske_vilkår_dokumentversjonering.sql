create table if not exists SYKDOM_DOKUMENT_INFORMASJON
(
    ID                      BIGINT                                  NOT NULL PRIMARY KEY,
    SYKDOM_DOKUMENT_ID      BIGINT                                  NOT NULL,
    VERSJON                 BIGINT                                  NOT NULL,
    TYPE                    VARCHAR(20)                             NOT NULL,
    DATERT                  DATE                                    ,
    MOTTATT                 TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'               NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP  NOT NULL
);
create sequence if not exists SEQ_SYKDOM_DOKUMENT_INFORMASJON increment by 5 minvalue 1000000;

insert into SYKDOM_DOKUMENT_INFORMASJON(ID, SYKDOM_DOKUMENT_ID, VERSJON, TYPE, DATERT, MOTTATT, OPPRETTET_AV, OPPRETTET_TID)
SELECT NEXTVAL('SEQ_SYKDOM_DOKUMENT_INFORMASJON'), ID, 0, TYPE, DATERT, MOTTATT, OPPRETTET_AV, OPPRETTET_TID
from SYKDOM_DOKUMENT;

alter table SYKDOM_DOKUMENT
    drop column TYPE,
    drop column DATERT,
    drop column MOTTATT,
    drop column ENDRET_AV,
    drop column ENDRET_TID;
