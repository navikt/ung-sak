-- Migration for Brukerdialog Oppgave tabeller (Varsel og Søknad)

-- Opprett tabell for brukerdialog varsel
create table BD_VARSEL
(
    id                      bigint not null primary key,
    oppgavereferanse               uuid not null,
    aktoer_id               varchar(50) not null,
    status                  varchar(50) not null,
    type                    varchar(100),
    data                    jsonb not null,
    frist_tid               timestamp,
    opprettet_tid           timestamp default CURRENT_TIMESTAMP not null,
    opprettet_av            varchar(20) not null default 'VL',
    endret_av               varchar(20),
    endret_tid              timestamp,
    versjon                 bigint default 0 not null
);

-- Opprett tabell for brukerdialog søknad
create table BD_SOKNAD
(
    id                      bigint not null primary key,
    oppgavereferanse               uuid not null,
    aktoer_id               varchar(50) not null,
    status                  varchar(50) not null,
    type                    varchar(100),
    data                    jsonb not null,
    opprettet_tid           timestamp default CURRENT_TIMESTAMP not null,
    opprettet_av            varchar(20) not null default 'VL',
    endret_av               varchar(20),
    endret_tid              timestamp,
    versjon                 bigint default 0 not null
);

-- Indekser for BD_VARSEL
create unique index idx_bd_varsel_aktoer_id
    on BD_VARSEL (aktoer_id);

create index idx_bd_varsel_status
    on BD_VARSEL (status);

create index idx_bd_varsel_type
    on BD_VARSEL (type);

create index idx_bd_varsel_frist_tid
    on BD_VARSEL (frist_tid) where status = 'ULØST';

-- Indekser for BD_SOKNAD
create unique index idx_bd_soknad_aktoer_id
    on BD_SOKNAD (aktoer_id);

create index idx_bd_soknad_status
    on BD_SOKNAD (status);

create index idx_bd_soknad_type
    on BD_SOKNAD (type);

-- Sekvenser
create sequence if not exists SEQ_BD_OPPGAVE increment by 50 minvalue 1000000;

-- Kommentarer for BD_VARSEL
comment on table BD_VARSEL is 'Inneholder brukerdialog varsler som sendes til bruker.';
comment on column BD_VARSEL.id is 'Primary Key. Unik identifikator for varsel.';
comment on column BD_VARSEL.aktoer_id is 'Aktør-ID for bruker som varslet gjelder.';
comment on column BD_VARSEL.status is 'Status på varsel (LØST, ULØST, AVBRUTT, UTLØPT, LUKKET).';
comment on column BD_VARSEL.type is 'Type varsel (BEKREFT_ENDRET_STARTDATO, BEKREFT_ENDRET_SLUTTDATO, etc).';
comment on column BD_VARSEL.data is 'JSON-data for varsel, inneholder polymorf oppgavedata avhengig av type.';
comment on column BD_VARSEL.frist_tid is 'Frist for å besvare varslet.';
comment on column BD_VARSEL.opprettet_tid is 'Tidspunkt for når varslet ble opprettet.';
comment on column BD_VARSEL.opprettet_av is 'Bruker/system som opprettet varslet.';
comment on column BD_VARSEL.endret_tid is 'Tidspunkt for når varslet sist ble endret.';
comment on column BD_VARSEL.endret_av is 'Bruker/system som sist endret varslet.';
comment on column BD_VARSEL.versjon is 'Versjonsnummer for optimistisk låsing.';

-- Kommentarer for BD_SOKNAD
comment on table BD_SOKNAD is 'Inneholder brukerdialog søknadsvarsler som sendes til bruker.';
comment on column BD_SOKNAD.id is 'Primary Key. Unik identifikator for søknad.';
comment on column BD_SOKNAD.aktoer_id is 'Aktør-ID for bruker som søknaden gjelder.';
comment on column BD_SOKNAD.status is 'Status på søknad (LØST, ULØST, AVBRUTT, UTLØPT, LUKKET).';
comment on column BD_SOKNAD.type is 'Type søknad (SØK_YTELSE, etc).';
comment on column BD_SOKNAD.data is 'JSON-data for søknad, inneholder polymorf oppgavedata avhengig av type.';
comment on column BD_SOKNAD.opprettet_tid is 'Tidspunkt for når søknaden ble opprettet.';
comment on column BD_SOKNAD.opprettet_av is 'Bruker/system som opprettet søknaden.';
comment on column BD_SOKNAD.endret_tid is 'Tidspunkt for når søknaden sist ble endret.';
comment on column BD_SOKNAD.endret_av is 'Bruker/system som sist endret søknaden.';
comment on column BD_SOKNAD.versjon is 'Versjonsnummer for optimistisk låsing.';

