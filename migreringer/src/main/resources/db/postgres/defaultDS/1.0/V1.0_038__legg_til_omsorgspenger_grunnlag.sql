create table if not exists OMP_OPPGITT_FRAVAER
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists OMP_OPPGITT_FRAVAER_PERIODE
(
    ID            				BIGINT                                 NOT NULL PRIMARY KEY,
    fravaer_id  	  			BIGINT REFERENCES OMP_OPPGITT_FRAVAER (id),
    FOM           				DATE                                   NOT NULL,
    TOM           				DATE                                   NOT NULL,
    fravaer_per_dag				VARCHAR(20),
    AKTIVITET_TYPE 				VARCHAR(100) 						   NOT NULL,
	ARBEIDSGIVER_AKTOR_ID 		VARCHAR(100),
	ARBEIDSGIVER_ORGNR 			VARCHAR(100),
	ARBEIDSFORHOLD_INTERN_ID 	UUID,
    VERSJON       BIGINT       	DEFAULT 0                 				NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  	DEFAULT 'VL'              				NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) 	DEFAULT CURRENT_TIMESTAMP 				NOT NULL,
    ENDRET_AV     				VARCHAR(20),
    ENDRET_TID    				TIMESTAMP(3)
);

create sequence if not exists SEQ_OMP_OPPGITT_FRAVAER_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_OMP_OPPGITT_FRAVAER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_GR_OMP_AKTIVITET increment by 50 minvalue 1000000;

create index IDX_OMP_OPPGITT_FRAVAER_PERIODE_01
    on OMP_OPPGITT_FRAVAER_PERIODE (FRAVAER_ID);

create table GR_OMP_AKTIVITET
(
    ID                   bigint                              not null PRIMARY KEY,
    BEHANDLING_ID        bigint                              not null,
    fravaer_id 	 bigint                              not null,
    VERSJON              bigint       default 0              not null,
    AKTIV                boolean      default true           not null,
    OPPRETTET_AV         VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID        TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV            VARCHAR(20),
    ENDRET_TID           TIMESTAMP(3),
    constraint FK_GR_OMP_AKTIVITET_01
        foreign key (BEHANDLING_ID) references behandling,
    constraint FK_GR_OMP_AKTIVITET_02
        foreign key (FRAVAER_ID) references OMP_OPPGITT_FRAVAER
);

create index IDX_GR_OMP_AKTIVITET_01
    on GR_OMP_AKTIVITET (BEHANDLING_ID);
create index IDX_GR_OMP_AKTIVITET_02
    on GR_OMP_AKTIVITET (fravaer_id);

CREATE UNIQUE INDEX UIDX_GR_OMP_AKTIVITET_01 ON GR_OMP_AKTIVITET (BEHANDLING_ID) WHERE (AKTIV=TRUE);

