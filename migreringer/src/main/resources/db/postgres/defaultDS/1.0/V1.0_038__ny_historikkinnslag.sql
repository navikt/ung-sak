DROP TABLE HISTORIKKINNSLAG_FELT;
DROP sequence SEQ_HISTORIKKINNSLAG_FELT;

DROP TABLE HISTORIKKINNSLAG_DEL;
DROP sequence SEQ_HISTORIKKINNSLAG_DEL;

DROP TABLE HISTORIKKINNSLAG_DOK_LINK;
DROP sequence SEQ_HISTORIKKINNSLAG_DOK_LINK;

DROP TABLE HISTORIKKINNSLAG;
DROP sequence SEQ_HISTORIKKINNSLAG;


create table HISTORIKKINNSLAG
(
    ID                      bigint      not null primary key,
    FAGSAK_ID               bigint                            not null references FAGSAK(ID),
    BEHANDLING_ID           bigint references BEHANDLING(ID),
    AKTOER                  varchar(100)                     not null,
    SKJERMLENKE             varchar(100),
    TITTEL                  varchar(1000),
    OPPRETTET_TID           timestamp default CURRENT_TIMESTAMP not null,
    OPPRETTET_AV            varchar(20) not null default 'VL',
    ENDRET_AV               varchar(20),
    ENDRET_TID              timestamp
);

comment on table HISTORIKKINNSLAG is 'Historikk over hendelser i saken';
comment on column HISTORIKKINNSLAG.ID is 'PK';
comment on column HISTORIKKINNSLAG.FAGSAK_ID is 'FK fagsak';
comment on column HISTORIKKINNSLAG.BEHANDLING_ID is 'FK behandling';
comment on column HISTORIKKINNSLAG.AKTOER is 'Hvilken aktoer';
comment on column HISTORIKKINNSLAG.SKJERMLENKE is 'Skjermlenke til endring i saken';
comment on column HISTORIKKINNSLAG.TITTEL is 'Tittel';

create index IDX_HISTORIKKINNSLAG_01
    on HISTORIKKINNSLAG (BEHANDLING_ID);

create index IDX_HISTORIKKINNSLAG_02
    on HISTORIKKINNSLAG (FAGSAK_ID);

create sequence SEQ_HISTORIKKINNSLAG
    minvalue 1000000
    increment by 50;

create table HISTORIKKINNSLAG_LINJE
(
    ID                  bigint                             not null primary key,
    HISTORIKKINNSLAG_ID bigint                             not null references HISTORIKKINNSLAG(ID),
    TYPE                varchar(100)                     not null,
    TEKST               varchar(4000),
    SEKVENS_NR          smallint                              not null,
    OPPRETTET_TID           timestamp default CURRENT_TIMESTAMP not null,
    OPPRETTET_AV            varchar(20) not null default 'VL',
    ENDRET_AV               varchar(20),
    ENDRET_TID              timestamp
);

comment on table HISTORIKKINNSLAG_LINJE is 'Linjer i historikkinnslag';
comment on column HISTORIKKINNSLAG_LINJE.ID is 'PK';
comment on column HISTORIKKINNSLAG_LINJE.TEKST is 'Innholdet. Forklarer hva som har skjedd i saken';
comment on column HISTORIKKINNSLAG_LINJE.SEKVENS_NR is 'Rekkefølge på linjer innad historikkinnslaget';
comment on column HISTORIKKINNSLAG_LINJE.TYPE is 'Type linje';

create index IDX_HISTORIKKINNSLAG_LINJE_01
    on HISTORIKKINNSLAG_LINJE (HISTORIKKINNSLAG_ID);

create sequence SEQ_HISTORIKKINNSLAG_LINJE
    minvalue 1000000
    increment by 50;

create table HISTORIKKINNSLAG_DOK_LINK
(
    ID                  bigint                             not null primary key,
    LINK_TEKST          varchar(100)                     not null,
    HISTORIKKINNSLAG_ID bigint                            not null references HISTORIKKINNSLAG(ID),
    JOURNALPOST_ID      varchar(100),
    DOKUMENT_ID         varchar(100),
    OPPRETTET_TID           timestamp default CURRENT_TIMESTAMP not null,
    OPPRETTET_AV            varchar(20) not null default 'VL',
    ENDRET_AV               varchar(20),
    ENDRET_TID              timestamp
);

create sequence SEQ_HISTORIKKINNSLAG_DOK_LINK
    minvalue 1000000
    increment by 50;

comment on table HISTORIKKINNSLAG_DOK_LINK is 'Kobling fra historikkinnslag til aktuell dokumentasjon';

comment on column HISTORIKKINNSLAG_DOK_LINK.ID is 'Primary Key';

comment on column HISTORIKKINNSLAG_DOK_LINK.LINK_TEKST is 'Tekst som vises for link til dokumentet';

comment on column HISTORIKKINNSLAG_DOK_LINK.HISTORIKKINNSLAG_ID is 'FK:HISTORIKKINNSLAG Fremmednøkkel til riktig innslag i historikktabellen';

comment on column HISTORIKKINNSLAG_DOK_LINK.JOURNALPOST_ID is 'Journalpost id';

comment on column HISTORIKKINNSLAG_DOK_LINK.DOKUMENT_ID is 'Dokument id';

create index IDX_HISTINNSLAG2_DOK_LINK_01
    on HISTORIKKINNSLAG_DOK_LINK (HISTORIKKINNSLAG_ID)
