create table if not exists GODKJENTE_OPPLAERINGSINSTITUSJONER
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    UUID                    UUID                                   NOT NULL,
    NAVN                    VARCHAR(100)                           NOT NULL,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3)
);
comment on table GODKJENTE_OPPLAERINGSINSTITUSJONER is 'Et register over forhåndsgodkjente opplæringsinstitusjoner til bruk i behandling av søknad om opplæringspenger';
create sequence if not exists SEQ_GODKJENTE_OPPLAERINGSINSTITUSJONER increment by 50 minvalue 1000000;
create unique index UIDX_GODKJENTE_OPPLAERINGSINSTITUSJONER_1 ON GODKJENTE_OPPLAERINGSINSTITUSJONER (NAVN);
create unique index UIDX_GODKJENTE_OPPLAERINGSINSTITUSJONER_2 ON GODKJENTE_OPPLAERINGSINSTITUSJONER (UUID);

create table if not exists GODKJENT_OPPLAERINGSINSTITUSJON_PERIODE
(
    ID                      BIGINT                                 NOT NULL PRIMARY KEY,
    INSTITUSJON_ID          BIGINT                                 NOT NULL,
    FOM                     DATE                                   NOT NULL,
    TOM                     DATE                                   NOT NULL,
    VERSJON                 BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV            VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID           TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV               VARCHAR(20),
    ENDRET_TID              TIMESTAMP(3),
    constraint FK_GODKJENT_OPPLAERINGSINSTITUSJON_PERIODE_1 foreign key (INSTITUSJON_ID) references GODKJENTE_OPPLAERINGSINSTITUSJONER,
    CONSTRAINT GODKJENT_OPPLAERINGSINSTITUSJON_PERIODE_IKKE_OVERLAPP EXCLUDE USING GIST (
        INSTITUSJON_ID WITH =,
        TSRANGE(FOM, TOM) WITH &&
        )
);
create sequence if not exists SEQ_GODKJENT_OPPLAERINGSINSTITUSJON_PERIODE increment by 50 minvalue 1000000;
