create table if not exists UT_UTTAK
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists UT_UTTAK_PERIODE
(
    ID            				BIGINT                                 NOT NULL PRIMARY KEY,
    UTTAK_ID  	  				BIGINT REFERENCES UT_UTTAK (id),
    FOM           				DATE                                   NOT NULL,
    TOM           				DATE                                   NOT NULL,
    UTTAK_AKTIVITET_TYPE 		VARCHAR(100) NOT NULL,
	ARBEIDSGIVER_AKTOR_ID 		VARCHAR(100),
	ARBEIDSGIVER_ORGNR 			VARCHAR(100),
	ARBEIDSFORHOLD_INTERN_ID 	UUID,
    VERSJON       BIGINT       	DEFAULT 0                 				NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  	DEFAULT 'VL'              				NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) 	DEFAULT CURRENT_TIMESTAMP 				NOT NULL,
    ENDRET_AV     				VARCHAR(20),
    ENDRET_TID    				TIMESTAMP(3)
);

create sequence if not exists SEQ_UT_UTTAK_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UT_UTTAK increment by 50 minvalue 1000000;
create sequence if not exists SEQ_GR_UTTAK increment by 50 minvalue 1000000;

create index IDX_UT_UTTAK_PERIODE_01
    on UT_UTTAK_PERIODE (UTTAK_ID);

create table GR_UTTAK
(
    ID                   bigint                              not null PRIMARY KEY,
    BEHANDLING_ID        bigint                              not null,
    oppgitt_uttak_id 	 bigint                              not null,
    VERSJON              bigint       default 0              not null,
    AKTIV                boolean      default true           not null,
    OPPRETTET_AV         VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID        TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV            VARCHAR(20),
    ENDRET_TID           TIMESTAMP(3),
    constraint FK_GR_UTTAK_01
        foreign key (BEHANDLING_ID) references behandling,
    constraint FK_GR_UTTAK_02
        foreign key (oppgitt_uttak_id) references UT_UTTAK
);

create index IDX_GR_UTTAK_01
    on GR_UTTAK (BEHANDLING_ID);
create index IDX_GR_UTTAK_02
    on GR_UTTAK (oppgitt_uttak_id);

CREATE UNIQUE INDEX UIDX_GR_UTTAK_01 ON GR_UTTAK (BEHANDLING_ID) WHERE (AKTIV=TRUE);

-- korriger indeks representasjon 
DROP INDEX UIDX_GR_FORDELING_01;
CREATE UNIQUE INDEX UIDX_GR_FORDELING_01 ON GR_FORDELING (BEHANDLING_ID) WHERE (AKTIV=TRUE);
