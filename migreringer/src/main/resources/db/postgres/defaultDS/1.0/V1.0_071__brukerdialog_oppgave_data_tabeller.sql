-- Migration: Separate tables for each OppgavetypeDataDTO subtype
-- Each table has its own PK (id) generated from a dedicated sequence,
-- and a FK oppgave_id referencing BD_OPPGAVE(id).
-- Audit columns (opprettet_av, opprettet_tid, endret_av, endret_tid) from BaseEntitet
-- are included in every table.

-- -------------------------------------------------------
-- 1. ENDRET_STARTDATO
-- -------------------------------------------------------
create sequence if not exists SEQ_BD_OPPGAVE_DATA_ENDRET_STARTDATO increment by 50 minvalue 1000000;

create table BD_OPPGAVE_DATA_ENDRET_STARTDATO
(
    id                  bigint      not null primary key,
    bd_oppgave_id       bigint      not null references BD_OPPGAVE (id),
    ny_startdato        date        not null,
    forrige_startdato   date        not null,
    opprettet_av        varchar(20) not null default 'VL',
    opprettet_tid       timestamp   not null default current_timestamp,
    endret_av           varchar(20),
    endret_tid          timestamp
);

create unique index idx_bd_data_endret_startdato_oppgave_id on BD_OPPGAVE_DATA_ENDRET_STARTDATO (bd_oppgave_id);

comment on table  BD_OPPGAVE_DATA_ENDRET_STARTDATO                       is 'Oppgavedata for type ENDRET_STARTDATO.';
comment on column BD_OPPGAVE_DATA_ENDRET_STARTDATO.id                    is 'Primary key.';
comment on column BD_OPPGAVE_DATA_ENDRET_STARTDATO.bd_oppgave_id         is 'FK til BD_OPPGAVE.id.';
comment on column BD_OPPGAVE_DATA_ENDRET_STARTDATO.ny_startdato          is 'Ny startdato.';
comment on column BD_OPPGAVE_DATA_ENDRET_STARTDATO.forrige_startdato     is 'Forrige startdato.';
comment on column BD_OPPGAVE_DATA_ENDRET_STARTDATO.opprettet_av          is 'Saksbehandler/system som opprettet raden.';
comment on column BD_OPPGAVE_DATA_ENDRET_STARTDATO.opprettet_tid         is 'Tidspunkt da raden ble opprettet.';
comment on column BD_OPPGAVE_DATA_ENDRET_STARTDATO.endret_av             is 'Saksbehandler/system som sist endret raden.';
comment on column BD_OPPGAVE_DATA_ENDRET_STARTDATO.endret_tid            is 'Tidspunkt da raden sist ble endret.';

-- -------------------------------------------------------
-- 2. ENDRET_SLUTTDATO
-- -------------------------------------------------------
create sequence if not exists SEQ_BD_OPPGAVE_DATA_ENDRET_SLUTTDATO increment by 50 minvalue 1000000;

create table BD_OPPGAVE_DATA_ENDRET_SLUTTDATO
(
    id                  bigint      not null primary key,
    bd_oppgave_id       bigint      not null references BD_OPPGAVE (id),
    ny_sluttdato        date        not null,
    forrige_sluttdato   date,
    opprettet_av        varchar(20) not null default 'VL',
    opprettet_tid       timestamp   not null default current_timestamp,
    endret_av           varchar(20),
    endret_tid          timestamp
);

create unique index idx_bd_data_endret_sluttdato_oppgave_id on BD_OPPGAVE_DATA_ENDRET_SLUTTDATO (bd_oppgave_id);

comment on table  BD_OPPGAVE_DATA_ENDRET_SLUTTDATO                       is 'Oppgavedata for type ENDRET_SLUTTDATO.';
comment on column BD_OPPGAVE_DATA_ENDRET_SLUTTDATO.id                    is 'Primary key.';
comment on column BD_OPPGAVE_DATA_ENDRET_SLUTTDATO.bd_oppgave_id         is 'FK til BD_OPPGAVE.id.';
comment on column BD_OPPGAVE_DATA_ENDRET_SLUTTDATO.ny_sluttdato          is 'Ny sluttdato.';
comment on column BD_OPPGAVE_DATA_ENDRET_SLUTTDATO.forrige_sluttdato     is 'Forrige sluttdato (null hvis det er første gang sluttdato settes).';
comment on column BD_OPPGAVE_DATA_ENDRET_SLUTTDATO.opprettet_av          is 'Saksbehandler/system som opprettet raden.';
comment on column BD_OPPGAVE_DATA_ENDRET_SLUTTDATO.opprettet_tid         is 'Tidspunkt da raden ble opprettet.';
comment on column BD_OPPGAVE_DATA_ENDRET_SLUTTDATO.endret_av             is 'Saksbehandler/system som sist endret raden.';
comment on column BD_OPPGAVE_DATA_ENDRET_SLUTTDATO.endret_tid            is 'Tidspunkt da raden sist ble endret.';

-- -------------------------------------------------------
-- 3. FJERNET_PERIODE
-- -------------------------------------------------------
create sequence if not exists SEQ_BD_OPPGAVE_DATA_FJERNET_PERIODE increment by 50 minvalue 1000000;

create table BD_OPPGAVE_DATA_FJERNET_PERIODE
(
    id                  bigint      not null primary key,
    bd_oppgave_id       bigint      not null references BD_OPPGAVE (id),
    forrige_startdato   date        not null,
    forrige_sluttdato   date,
    opprettet_av        varchar(20) not null default 'VL',
    opprettet_tid       timestamp   not null default current_timestamp,
    endret_av           varchar(20),
    endret_tid          timestamp
);

create unique index idx_bd_data_fjernet_periode_oppgave_id on BD_OPPGAVE_DATA_FJERNET_PERIODE (bd_oppgave_id);

comment on table  BD_OPPGAVE_DATA_FJERNET_PERIODE                        is 'Oppgavedata for type FJERNET_PERIODE.';
comment on column BD_OPPGAVE_DATA_FJERNET_PERIODE.id                     is 'Primary key.';
comment on column BD_OPPGAVE_DATA_FJERNET_PERIODE.bd_oppgave_id          is 'FK til BD_OPPGAVE.id.';
comment on column BD_OPPGAVE_DATA_FJERNET_PERIODE.forrige_startdato      is 'Startdato på perioden som ble fjernet.';
comment on column BD_OPPGAVE_DATA_FJERNET_PERIODE.forrige_sluttdato      is 'Sluttdato på perioden som ble fjernet (null = åpen periode).';
comment on column BD_OPPGAVE_DATA_FJERNET_PERIODE.opprettet_av           is 'Saksbehandler/system som opprettet raden.';
comment on column BD_OPPGAVE_DATA_FJERNET_PERIODE.opprettet_tid          is 'Tidspunkt da raden ble opprettet.';
comment on column BD_OPPGAVE_DATA_FJERNET_PERIODE.endret_av              is 'Saksbehandler/system som sist endret raden.';
comment on column BD_OPPGAVE_DATA_FJERNET_PERIODE.endret_tid             is 'Tidspunkt da raden sist ble endret.';

-- -------------------------------------------------------
-- 4. ENDRET_PERIODE
-- -------------------------------------------------------
create sequence if not exists SEQ_BD_OPPGAVE_DATA_ENDRET_PERIODE increment by 50 minvalue 1000000;

create table BD_OPPGAVE_DATA_ENDRET_PERIODE
(
    id                          bigint       not null primary key,
    bd_oppgave_id               bigint       not null references BD_OPPGAVE (id),
    ny_periode_fom              date,
    ny_periode_tom              date,
    forrige_periode_fom         date,
    forrige_periode_tom         date,
    -- Kommaseparert liste av PeriodeEndringType-verdier (ENDRET_STARTDATO, ENDRET_SLUTTDATO, FJERNET_PERIODE, ANDRE_ENDRINGER)
    endringer                   varchar(255) not null,
    opprettet_av                varchar(20)  not null default 'VL',
    opprettet_tid               timestamp    not null default current_timestamp,
    endret_av                   varchar(20),
    endret_tid                  timestamp
);

create unique index idx_bd_data_endret_periode_oppgave_id on BD_OPPGAVE_DATA_ENDRET_PERIODE (bd_oppgave_id);

comment on table  BD_OPPGAVE_DATA_ENDRET_PERIODE                         is 'Oppgavedata for type ENDRET_PERIODE.';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.id                      is 'Primary key.';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.bd_oppgave_id           is 'FK til BD_OPPGAVE.id.';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.ny_periode_fom          is 'Fra-dato for ny periode (null hvis perioden er fjernet).';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.ny_periode_tom          is 'Til-dato for ny periode.';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.forrige_periode_fom     is 'Fra-dato for forrige periode (null hvis ny).';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.forrige_periode_tom     is 'Til-dato for forrige periode.';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.endringer               is 'Kommaseparert liste av PeriodeEndringType-verdier som beskriver hva som har endret seg.';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.opprettet_av            is 'Saksbehandler/system som opprettet raden.';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.opprettet_tid           is 'Tidspunkt da raden ble opprettet.';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.endret_av               is 'Saksbehandler/system som sist endret raden.';
comment on column BD_OPPGAVE_DATA_ENDRET_PERIODE.endret_tid              is 'Tidspunkt da raden sist ble endret.';

-- -------------------------------------------------------
-- 5. KONTROLLER_REGISTERINNTEKT
-- -------------------------------------------------------
create sequence if not exists SEQ_BD_OPPGAVE_DATA_KONTROLLER_REG_INNTEKT increment by 50 minvalue 1000000;

create table BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT
(
    id                              bigint      not null primary key,
    bd_oppgave_id                   bigint      not null references BD_OPPGAVE (id),
    fra_og_med                      date        not null,
    til_og_med                      date        not null,
    gjelder_deler_av_maaned         boolean     not null,
    total_inntekt_arbeid_frilans    integer     not null,
    total_inntekt_ytelse            integer     not null,
    total_inntekt                   integer     not null,
    opprettet_av                    varchar(20) not null default 'VL',
    opprettet_tid                   timestamp   not null default current_timestamp,
    endret_av                       varchar(20),
    endret_tid                      timestamp
);

create unique index idx_bd_data_kontroller_reg_inntekt_oppgave_id on BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT (bd_oppgave_id);

comment on table  BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT                               is 'Oppgavedata for type KONTROLLER_REGISTERINNTEKT.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.id                            is 'Primary key.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.bd_oppgave_id                 is 'FK til BD_OPPGAVE.id.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.fra_og_med                   is 'Startdato for perioden inntekten gjelder.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.til_og_med                   is 'Sluttdato for perioden inntekten gjelder.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.gjelder_deler_av_maaned      is 'Sant dersom perioden ikke dekker hele måneden.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.total_inntekt_arbeid_frilans is 'Sum arbeid- og frilansinntekt.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.total_inntekt_ytelse         is 'Sum ytelse-inntekt.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.total_inntekt                is 'Total inntekt (arbeid + frilans + ytelse).';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.opprettet_av                 is 'Saksbehandler/system som opprettet raden.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.opprettet_tid                is 'Tidspunkt da raden ble opprettet.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.endret_av                    is 'Saksbehandler/system som sist endret raden.';
comment on column BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.endret_tid                   is 'Tidspunkt da raden sist ble endret.';

-- Enkeltposter for arbeid/frilans-inntekter knyttet til oppgaven
create table BD_OPPGAVE_DATA_ARBEID_FRILANS_INNTEKT
(
    id                  bigserial    not null primary key,
    kontroller_data_id  bigint       not null references BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT (id),
    arbeidsgiver        varchar(255) not null,
    inntekt             integer      not null
);

create index idx_bd_arbeid_frilans_inntekt_oppgave_id on BD_OPPGAVE_DATA_ARBEID_FRILANS_INNTEKT (kontroller_data_id);

comment on table  BD_OPPGAVE_DATA_ARBEID_FRILANS_INNTEKT                     is 'Arbeid- og frilansinntekter knyttet til en KONTROLLER_REGISTERINNTEKT-oppgave.';
comment on column BD_OPPGAVE_DATA_ARBEID_FRILANS_INNTEKT.id                  is 'Primary key.';
comment on column BD_OPPGAVE_DATA_ARBEID_FRILANS_INNTEKT.kontroller_data_id  is 'FK til BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.id.';
comment on column BD_OPPGAVE_DATA_ARBEID_FRILANS_INNTEKT.arbeidsgiver        is 'Arbeidsgiver-identifikator (orgnummer eller personnummer).';
comment on column BD_OPPGAVE_DATA_ARBEID_FRILANS_INNTEKT.inntekt             is 'Inntektsbeløp i hele kroner.';

-- Enkeltposter for ytelse-inntekter knyttet til oppgaven
create table BD_OPPGAVE_DATA_YTELSE_INNTEKT
(
    id                  bigserial    not null primary key,
    kontroller_data_id  bigint       not null references BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT (id),
    ytelsetype          varchar(100) not null,
    inntekt             integer      not null
);

create index idx_bd_ytelse_inntekt_oppgave_id on BD_OPPGAVE_DATA_YTELSE_INNTEKT (kontroller_data_id);

comment on table  BD_OPPGAVE_DATA_YTELSE_INNTEKT                             is 'Ytelse-inntekter knyttet til en KONTROLLER_REGISTERINNTEKT-oppgave.';
comment on column BD_OPPGAVE_DATA_YTELSE_INNTEKT.id                          is 'Primary key.';
comment on column BD_OPPGAVE_DATA_YTELSE_INNTEKT.kontroller_data_id          is 'FK til BD_OPPGAVE_DATA_KONTROLLER_REGISTERINNTEKT.id.';
comment on column BD_OPPGAVE_DATA_YTELSE_INNTEKT.ytelsetype                  is 'Ytelsetype-kode (YtelseType enum).';
comment on column BD_OPPGAVE_DATA_YTELSE_INNTEKT.inntekt                     is 'Inntektsbeløp i hele kroner.';

-- -------------------------------------------------------
-- 6. INNTEKTSRAPPORTERING
-- -------------------------------------------------------
create sequence if not exists SEQ_BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING increment by 50 minvalue 1000000;

create table BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING
(
    id                          bigint      not null primary key,
    bd_oppgave_id               bigint      not null references BD_OPPGAVE (id),
    fra_og_med                  date        not null,
    til_og_med                  date        not null,
    gjelder_deler_av_maaned     boolean     not null,
    -- Rapportert inntekt (null inntil bruker har svart)
    rapportert_inntekt          integer,
    opprettet_av                varchar(20) not null default 'VL',
    opprettet_tid               timestamp   not null default current_timestamp,
    endret_av                   varchar(20),
    endret_tid                  timestamp
);

create unique index idx_bd_data_inntektsrapportering_oppgave_id on BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING (bd_oppgave_id);

comment on column BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING.id                           is 'Primary key.';
comment on column BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING.bd_oppgave_id               is 'FK til BD_OPPGAVE.id.';
comment on column BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING.fra_og_med                  is 'Startdato for rapporteringsperioden.';
comment on column BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING.til_og_med                  is 'Sluttdato for rapporteringsperioden.';
comment on column BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING.gjelder_deler_av_maaned     is 'Sant dersom perioden ikke dekker hele måneden.';
comment on column BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING.rapportert_inntekt          is 'Inntekt rapportert av bruker (null = ikke rapportert ennå).';
comment on column BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING.opprettet_av                is 'Saksbehandler/system som opprettet raden.';
comment on column BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING.opprettet_tid               is 'Tidspunkt da raden ble opprettet.';
comment on column BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING.endret_av                   is 'Saksbehandler/system som sist endret raden.';
comment on column BD_OPPGAVE_DATA_INNTEKTSRAPPORTERING.endret_tid                  is 'Tidspunkt da raden sist ble endret.';

-- -------------------------------------------------------
-- 7. SØK_YTELSE
-- -------------------------------------------------------
create sequence if not exists SEQ_BD_OPPGAVE_DATA_SOK_YTELSE increment by 50 minvalue 1000000;

create table BD_OPPGAVE_DATA_SOK_YTELSE
(
    id              bigint      not null primary key,
    bd_oppgave_id   bigint      not null references BD_OPPGAVE (id),
    fom_dato        date        not null,
    opprettet_av    varchar(20) not null default 'VL',
    opprettet_tid   timestamp   not null default current_timestamp,
    endret_av       varchar(20),
    endret_tid      timestamp
);

create unique index idx_bd_data_sok_ytelse_oppgave_id on BD_OPPGAVE_DATA_SOK_YTELSE (bd_oppgave_id);

comment on table  BD_OPPGAVE_DATA_SOK_YTELSE              is 'Oppgavedata for type SØK_YTELSE.';
comment on column BD_OPPGAVE_DATA_SOK_YTELSE.id            is 'Primary key.';
comment on column BD_OPPGAVE_DATA_SOK_YTELSE.bd_oppgave_id is 'FK til BD_OPPGAVE.id.';
comment on column BD_OPPGAVE_DATA_SOK_YTELSE.fom_dato      is 'Fra-og-med-dato for når bruker kan søke ytelsen.';
comment on column BD_OPPGAVE_DATA_SOK_YTELSE.opprettet_av  is 'Saksbehandler/system som opprettet raden.';
comment on column BD_OPPGAVE_DATA_SOK_YTELSE.opprettet_tid is 'Tidspunkt da raden ble opprettet.';
comment on column BD_OPPGAVE_DATA_SOK_YTELSE.endret_av     is 'Saksbehandler/system som sist endret raden.';
comment on column BD_OPPGAVE_DATA_SOK_YTELSE.endret_tid    is 'Tidspunkt da raden sist ble endret.';
