-- Migration for Brukerdialog Oppgave tabell

-- Opprett tabell for brukerdialog oppgaver
create table BD_OPPGAVE
(
    id                      bigint not null primary key,
    oppgavereferanse        uuid not null,
    aktoer_id               varchar(50) not null,
    status                  varchar(50) not null,
    type                    varchar(100),
    data                    jsonb not null,
    frist_tid               timestamp,
    løst_dato               timestamp,
    åpnet_dato              timestamp,
    lukket_dato             timestamp,
    opprettet_tid           timestamp default CURRENT_TIMESTAMP not null,
    opprettet_av            varchar(20) not null default 'VL',
    endret_av               varchar(20),
    endret_tid              timestamp,
    versjon                 bigint default 0 not null
);

-- Indekser for BD_OPPGAVE
create unique index idx_bd_oppgave_oppgavereferanse
    on BD_OPPGAVE (oppgavereferanse);

create index idx_bd_oppgave_aktoer_id
    on BD_OPPGAVE (aktoer_id);

create index idx_bd_oppgave_status
    on BD_OPPGAVE (status);

create index idx_bd_oppgave_type
    on BD_OPPGAVE (type);

create index idx_bd_oppgave_frist_tid
    on BD_OPPGAVE (frist_tid) where status = 'ULØST';

-- Sekvens
create sequence if not exists SEQ_BD_OPPGAVE increment by 50 minvalue 1000000;

-- Kommentarer for BD_OPPGAVE
comment on table BD_OPPGAVE is 'Inneholder brukerdialog-oppgaver som sendes til bruker.';
comment on column BD_OPPGAVE.id is 'Primary Key. Unik identifikator for oppgave.';
comment on column BD_OPPGAVE.aktoer_id is 'Aktør-ID for bruker som oppgaven gjelder.';
comment on column BD_OPPGAVE.oppgavereferanse is 'Unik ekstern referanse til oppgaven.';
comment on column BD_OPPGAVE.status is 'Status for oppgaven (ULØST, LØST, LUKKET, AVBRUTT, UTLØPT).';
comment on column BD_OPPGAVE.type is 'Type oppgave.';
comment on column BD_OPPGAVE.data is 'JSON-data med oppgavedetaljer.';
comment on column BD_OPPGAVE.frist_tid is 'Frist for når oppgaven må være løst.';
comment on column BD_OPPGAVE.løst_dato is 'Tidspunkt når oppgaven ble løst.';
comment on column BD_OPPGAVE.åpnet_dato is 'Tidspunkt når oppgaven ble åpnet av bruker.';
comment on column BD_OPPGAVE.lukket_dato is 'Tidspunkt når oppgaven ble lukket.';
comment on column BD_OPPGAVE.frist_tid is 'Frist for å besvare oppgaven.';
comment on column BD_OPPGAVE.opprettet_tid is 'Tidspunkt for når varslet ble opprettet.';
comment on column BD_OPPGAVE.opprettet_av is 'Bruker/system som opprettet varslet.';
comment on column BD_OPPGAVE.endret_tid is 'Tidspunkt for når varslet sist ble endret.';
comment on column BD_OPPGAVE.endret_av is 'Bruker/system som sist endret varslet.';
comment on column BD_OPPGAVE.versjon is 'Versjonsnummer for optimistisk låsing.';
