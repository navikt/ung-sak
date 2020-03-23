-- Legg til tilsynsordning som nye tabeller
alter table GR_UTTAK 
  add column tilsynsordning_id bigint;
  
create table if not exists UT_TILSYNSORDNING
(
    ID            		BIGINT                                 NOT NULL PRIMARY KEY,
    TILSYNSORDNING_SVAR VARCHAR(10),
    VERSJON       		BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  		VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID 		TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV    		VARCHAR(20),
    ENDRET_TID    		TIMESTAMP(3)
);

create table if not exists UT_TILSYNSORDNING_PERIODE
(
    ID            				BIGINT                        			NOT NULL PRIMARY KEY,
    TILSYNSORDNING_ID  	  		BIGINT 					 				NOT NULL REFERENCES UT_TILSYNSORDNING (id),
    FOM           				DATE                              	    NOT NULL,
    TOM           				DATE                              	    NOT NULL,
    VARIGHET					VARCHAR(20)								NOT NULL, -- angir varighet(duratin) ISO8601 std. eks. PT30M
    VERSJON       BIGINT       	DEFAULT 0                 				NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  	DEFAULT 'VL'              				NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) 	DEFAULT CURRENT_TIMESTAMP 				NOT NULL,
    ENDRET_AV     				VARCHAR(20),
    ENDRET_TID    				TIMESTAMP(3)
);

create sequence if not exists SEQ_UT_TILSYNSORDNING_PERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_UT_TILSYNSORDNING increment by 50 minvalue 1000000;

create index IDX_UT_TILSYNSORDNING_PERIODE_01 on UT_TILSYNSORDNING_PERIODE (TILSYNSORDNING_ID);

create index IDX_GR_UTTAK_04 on GR_UTTAK (tilsynsordning_id);

alter table GR_UTTAK add constraint FK_GR_UTTAK_06 FOREIGN KEY (tilsynsordning_id) REFERENCES UT_TILSYNSORDNING(id);

