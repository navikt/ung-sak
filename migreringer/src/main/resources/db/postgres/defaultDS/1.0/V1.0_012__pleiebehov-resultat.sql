create table if not exists PB_PLEIEPERIODER
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists PB_PLEIEPERIODE
(
    ID                     BIGINT                                 NOT NULL PRIMARY KEY,
    PLEIEPERIODER_ID BIGINT REFERENCES PB_PLEIEPERIODER (id),
    FOM                    DATE                                   NOT NULL,
    TOM                    DATE                                   NOT NULL,
    GRAD                   INT                                    NOT NULL,
    VERSJON                BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV           VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID          TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3)
);
create index IDX_PB_PLEIEPERIODE_1
    on PB_PLEIEPERIODE (PLEIEPERIODER_ID);

create sequence if not exists SEQ_PB_PLEIEPERIODER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_PB_PLEIEPERIODE increment by 50 minvalue 1000000;
create sequence if not exists SEQ_RS_PLEIEBEHOV increment by 50 minvalue 1000000;


create table RS_PLEIEBEHOV
(
    ID                     bigint                              not null PRIMARY KEY,
    BEHANDLING_ID          bigint                              not null,
    PLEIEPERIODER_ID bigint,
    VERSJON                bigint       default 0              not null,
    AKTIV                  boolean      default true           not null,
    OPPRETTET_AV           VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID          TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3),
    constraint FK_RS_PLEIEBEHOV_1
        foreign key (BEHANDLING_ID) references BEHANDLING,
    constraint FK_RS_PLEIEBEHOV_2
        foreign key (PLEIEPERIODER_ID) references PB_PLEIEPERIODER
);

create index IDX_RS_PLEIEBEHOV_1
    on RS_PLEIEBEHOV (BEHANDLING_ID);
create index IDX_RS_PLEIEBEHOV_2
    on RS_PLEIEBEHOV (PLEIEPERIODER_ID);


CREATE UNIQUE INDEX UIDX_RS_PLEIEBEHOV_01
    ON RS_PLEIEBEHOV (
                     (CASE
                          WHEN AKTIV = true
                              THEN BEHANDLING_ID
                          ELSE NULL END),
                     (CASE
                          WHEN AKTIV = true
                              THEN AKTIV
                          ELSE NULL END)
        );
