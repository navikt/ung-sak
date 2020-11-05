create table if not exists MAN_VV_FRITEKST
(
    ID            				BIGINT                                  NOT NULL PRIMARY KEY,
    fritekst				    TEXT                                    NOT NULL,
    VERSJON       BIGINT       	DEFAULT 0                 				NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  	DEFAULT 'VL'              				NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) 	DEFAULT CURRENT_TIMESTAMP 				NOT NULL,
    ENDRET_AV     				VARCHAR(20),
    ENDRET_TID    				TIMESTAMP(3)
);

create sequence if not exists SEQ_MAN_VV_FRITEKST increment by 50 minvalue 1000000;

create table if not exists GR_MAN_VILKARSVURDERING
(
    ID                   bigint                              not null PRIMARY KEY,
    BEHANDLING_ID        bigint                              not null,
    fritekst_id          bigint                              not null,
    VERSJON              bigint       default 0              not null,
    AKTIV                boolean      default true           not null,
    OPPRETTET_AV         VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID        TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV            VARCHAR(20),
    ENDRET_TID           TIMESTAMP(3),
    constraint FK_GR_MAN_VILKARSVURDERING_01
        foreign key (BEHANDLING_ID) references behandling,
    constraint FK_GR_MAN_VILKARSVURDERING_02
        foreign key (fritekst_id) references MAN_VV_FRITEKST
);

create index IDX_GR_MAN_VILKARSVURDERING_01
    on GR_MAN_VILKARSVURDERING (BEHANDLING_ID);
create index IDX_GR_MAN_VILKARSVURDERING_02
    on GR_MAN_VILKARSVURDERING (fritekst_id);

create sequence if not exists SEQ_GR_MAN_VILKARSVURDERING increment by 50 minvalue 1000000;
