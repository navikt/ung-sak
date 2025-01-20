create table fordel_mottatt_melding
(
    id                   bigint                           not null
        primary key,
    journalpostid        varchar(50)                      not null,
    tema                 varchar(50),
    behandlingstype      varchar(50),
    behandlingstema      varchar(50),
    brevkode             varchar(50),
    soeknad_id           varchar(50),
    opprettet_tid        timestamp default CURRENT_TIMESTAMP not null,
    endret_tid           timestamp,
    payload              text
);

comment on table fordel_mottatt_melding is 'Metadata fra mottatte meldinger per journalpostid';
comment on column fordel_mottatt_melding.journalpostid is 'journalpostid - naturlig nøkkel for tabellen';
comment on column fordel_mottatt_melding.tema is 'tema fra joark';
comment on column fordel_mottatt_melding.behandlingstype is 'behandlingstype fra joark';
comment on column fordel_mottatt_melding.behandlingstema is 'behandlingstema fra joark';
comment on column fordel_mottatt_melding.brevkode is 'brevkode fra joark';
comment on column fordel_mottatt_melding.soeknad_id is 'søknad id generert av system som har lagd søknad';

create unique index uidx_fordel_mottatt_melding_journalpostid
    on fordel_mottatt_melding (journalpostid);

create index uidx_fordel_mottatt_melding_soeknadid_tema_behandlingstema
    on fordel_mottatt_melding (soeknad_id, tema, behandlingstema);

create index idx_fordel_mottatt_melding_brevkode
    on fordel_mottatt_melding (brevkode);


create table fordel_journalpost_innsending
(
    journalpost_id       varchar(50)  not null
        constraint pk_fordel_journalpost_innsending
            primary key,
    ytelse_type          varchar(50)  not null,
    aktoer_id            varchar(50)  not null,
    saksnummer           varchar(50)  not null,
    status               varchar(20)  not null,
    brevkode             varchar(50)  not null,
    opprettet_tid        timestamp default LOCALTIMESTAMP,
    endret_tid           timestamp,
    innsendingstidspunkt timestamp,
    payload              text
);

comment on table fordel_journalpost_innsending is 'Inneholder hvilke journalposter oversendes til saksystem.';
comment on column fordel_journalpost_innsending.journalpost_id is 'Primary Key. Journalposten denne tabellen inneholder ekstra informasjon om.';
comment on column fordel_journalpost_innsending.ytelse_type is 'Ytelsetype (fagsak ytelsetype) som journalpost gjelder.';
comment on column fordel_journalpost_innsending.aktoer_id is 'Aktøren som journalpost gjelder for.';
comment on column fordel_journalpost_innsending.saksnummer is 'Saksnummer som journalpost gjelder for.';
comment on column fordel_journalpost_innsending.status is 'Status på søknaden: UBEHANDLET, INNSENDT';
comment on column fordel_journalpost_innsending.brevkode is 'Brevkode (fra saf/joark) for journalpost.';
comment on column fordel_journalpost_innsending.innsendingstidspunkt is 'Tidspunkt melding var originalt sendt inn til Nav (enten fra kildesystem) eller tidspunkt mottatt (dersom første ikke er angitt i melding)';
comment on column fordel_journalpost_innsending.payload is 'payload fra journal';

create unique index uidx_fordel_journalpost_innsending_jpid
    on fordel_journalpost_innsending (journalpost_id);

create index idx_fordel_journalpost_innsending_aktoerid
    on fordel_journalpost_innsending (aktoer_id);

create index idx_fordel_journalpost_innsending_saksnr
    on fordel_journalpost_innsending (saksnummer);

create index idx_fordel_journalpost_innsending_opprtid
    on fordel_journalpost_innsending (opprettet_tid);

create table fordel_produksjonsstyring_oppgave
(
    journalpost_id    varchar(50)                            not null
        constraint pk_fordel_produksjonsstyring_oppgave
            primary key,
    aktoer_id         varchar(50),
    ytelse_type       varchar(50) not null,
    fagsak_system     varchar(50),
    behandlingstema   varchar(50),
    oppgave_type      varchar(50)                            not null,
    oppgave_id        varchar(50),
    beskrivelse       text,
    opprettet_tid     timestamp(3) default CURRENT_TIMESTAMP not null,
    endret_tid        timestamp(3)
);

comment on table fordel_produksjonsstyring_oppgave is 'Inneholder sporingsinformasjon for oppgaver opprettet i Gosys.';
comment on column fordel_produksjonsstyring_oppgave.journalpost_id is 'Primary Key. Journalposten oppgaven er opprettet for.';
comment on column fordel_produksjonsstyring_oppgave.aktoer_id is 'Aktøren som oppgaven gjelder for.';
comment on column fordel_produksjonsstyring_oppgave.ytelse_type is 'FagsakYtelseType oppgave gjelder';
comment on column fordel_produksjonsstyring_oppgave.fagsak_system is 'Fagsaksystem oppgave skal knyttes til gjennom Gosys (IT00, FS39, ): Offisiell kode';
comment on column fordel_produksjonsstyring_oppgave.oppgave_type is 'OppgaveType oppgave skal knyttes til i Gosys (GEN, JFR, VUR, ): Offisiell kode';
comment on column fordel_produksjonsstyring_oppgave.oppgave_id is 'Oppgave id tilknyttet gjennom Gosys';
comment on column fordel_produksjonsstyring_oppgave.beskrivelse is 'Beskrivelse tilordnet oppgave (hint til saksbehandler)';

create unique index uidx_fordel_produksjonsstyring_oppgave_jpid
    on fordel_produksjonsstyring_oppgave (journalpost_id);

create unique index uidx_fordel_produksjonsstyring_oppgave_oppgid
    on fordel_produksjonsstyring_oppgave (oppgave_id);

create index idx_fordel_produksjonsstyring_oppgave_aktorid
    on fordel_produksjonsstyring_oppgave (aktoer_id);

create table fordel_journalpost_mottatt
(
    journalpost_id    varchar(50) not null
        constraint pk_fordel_journalpost_mottatt
            primary key,
    aktoer_id         varchar(50),
    kanal             varchar(100),
    tittel            varchar(4000),
    status            varchar(20) not null,
    brevkode          varchar(50),
    behandling_tema   varchar(50),
    mottatt_tidspunkt timestamp   not null,
    payload           text,
    opprettet_tid     timestamp default CURRENT_TIMESTAMP,
    endret_tid        timestamp
);

comment on table fordel_journalpost_mottatt is 'Inneholder hvilke journalposter mottatt i fordel.';
comment on column fordel_journalpost_mottatt.journalpost_id is 'Primary Key. Journalposten denne tabellen inneholder ekstra informasjon om.';
comment on column fordel_journalpost_mottatt.aktoer_id is 'Aktøren som journalpost gjelder for.';
comment on column fordel_journalpost_mottatt.kanal is 'Kanal journalpost innsendt på.';
comment on column fordel_journalpost_mottatt.status is 'Status på søknaden: UBEHANDLET, INNSENDT';
comment on column fordel_journalpost_mottatt.brevkode is 'Brevkode (fra saf/joark) for journalpost.';
comment on column fordel_journalpost_mottatt.behandling_tema is 'BehandlingTema journalpost er registrert på.';
comment on column fordel_journalpost_mottatt.mottatt_tidspunkt is 'Tidspunkt melding var originalt sendt inn til Nav (enten fra kildesystem) eller tidspunkt mottatt';
comment on column fordel_journalpost_mottatt.payload is 'Innhold som skal sendes til fagsystem';



create unique index uidx_fordel_journalpost_mottatt_jpid
    on fordel_journalpost_mottatt (journalpost_id);

create index idx_fordel_journalpost_mottatt_aktorid
    on fordel_journalpost_mottatt (aktoer_id);

create index idx_fordel_journalpost_mottatt_opprtid
    on fordel_journalpost_mottatt (opprettet_tid);

create index idx_fordel_journalpost_mottatt_mottatid
    on fordel_journalpost_mottatt (mottatt_tidspunkt);

create table fordel_inngaaende_hendelse
(
    id                       bigint                                       not null
        primary key,
    hendelse_id              varchar(100)                                 not null,
    type                     varchar(100)                                 not null,
    aktoer_id                varchar(100)                                 not null,
    melding_opprettet        timestamp(3),
    payload                  text,
    haandtert_status         varchar(100) default 'MOTTATT'::character varying,
    haandtert_av_hendelse_id varchar(100),
    opprettet_av             varchar(20)  default 'VL'::character varying not null,
    opprettet_tid            timestamp(3) default CURRENT_TIMESTAMP    not null,
    endret_av                varchar(20),
    endret_tid               timestamp(3)
);

create index idx_fordel_inngaaende_hendelse_aktoer_id_aktorid
    on fordel_inngaaende_hendelse (aktoer_id);


create sequence seq_fordel_mottatt_melding
    minvalue 10000000
    increment by 50;

create sequence seq_fordel_inngaaende_hendelse
    minvalue 1000000
    increment by 50;
