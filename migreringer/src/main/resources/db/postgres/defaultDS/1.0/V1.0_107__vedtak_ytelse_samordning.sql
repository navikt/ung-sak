-- Vedtak om ungdomsprogramytelse og aktivitetspenger til samordning med andre livsoppholdsytelser (TSFF-3395).
-- Modellen følger vedtak_ytelse/ve_ytelse_anvist i abakus, med disse avvikene:
--   * daterange i stedet for fom/tom
--   * ingen ve_ytelse_andel (alltid 1:1 med anvisningen i ung-sak, bygges ved mapping)
--   * barnetillegg per anvisning
--   * status og kilde lagres ikke (status utledes fra fagsaken ved oppslag, kilde er alltid ung-sak)
--   * eldre vedtak forkastes ikke, men lagres som inaktive rader

create sequence seq_vedtak_ytelse increment by 50 minvalue 1000000;
create sequence seq_ve_ytelse_anvist increment by 50 minvalue 1000000;

create table vedtak_ytelse
(
    id                bigint primary key,
    aktoer_id         varchar(50)  not null,
    saksnummer        varchar(19)  not null,
    ytelse_type       varchar(100) not null,
    vedtak_referanse  uuid         not null,
    vedtatt_tidspunkt timestamp(3) not null,
    periode           daterange    not null,
    aktiv             boolean      not null default false,
    versjon           bigint       not null default 0,
    opprettet_av      varchar(20)  not null default 'VL',
    opprettet_tid     timestamp(3) not null default current_timestamp,
    endret_av         varchar(20),
    endret_tid        timestamp(3)
);

comment on table vedtak_ytelse is 'Iverksatte vedtak om ungdomsprogramytelse og aktivitetspenger, til bruk i samordning med andre ytelser i Nav.';
comment on column vedtak_ytelse.aktoer_id is 'Aktørid til brukeren vedtaket gjelder';
comment on column vedtak_ytelse.saksnummer is 'Saksnummer til fagsaken vedtaket hører til';
comment on column vedtak_ytelse.ytelse_type is 'Fagsakens ytelsestype (UNG eller AVP)';
comment on column vedtak_ytelse.vedtak_referanse is 'Uuid til behandlingen som ga vedtaket. Lagring med samme referanse erstatter raden.';
comment on column vedtak_ytelse.vedtatt_tidspunkt is 'Når vedtaket ble fattet. Avgjør hvilket vedtak som er aktivt for saken.';
comment on column vedtak_ytelse.periode is 'Første til siste innvilgede dag. Vedtaksdatoen når vedtaket ikke har noen innvilget periode.';
comment on column vedtak_ytelse.aktiv is 'Om vedtaket er det gjeldende for saken. Kun ett aktivt vedtak per sak og ytelse.';

create unique index uidx_vedtak_ytelse_referanse on vedtak_ytelse (vedtak_referanse);
create unique index uidx_vedtak_ytelse_aktiv on vedtak_ytelse (saksnummer, ytelse_type) where aktiv;
create index idx_vedtak_ytelse_aktoer on vedtak_ytelse (aktoer_id);

create table ve_ytelse_anvist
(
    id                   bigint primary key,
    vedtak_ytelse_id     bigint       not null references vedtak_ytelse (id),
    periode              daterange    not null,
    dagsats              numeric(19, 2),
    beloep               numeric(19, 2),
    utbetalingsgrad      numeric(5, 2),
    antall_barn          integer,
    dagsats_barnetillegg integer,
    opprettet_av         varchar(20)  not null default 'VL',
    opprettet_tid        timestamp(3) not null default current_timestamp,
    endret_av            varchar(20),
    endret_tid           timestamp(3)
);

comment on table ve_ytelse_anvist is 'Innvilgede delperioder i et vedtak. Ny rad der beregnet ytelse eller barnetillegg endres.';
comment on column ve_ytelse_anvist.periode is 'Innvilget delperiode';
comment on column ve_ytelse_anvist.dagsats is 'Beregnet dagsats. Null når perioden ikke er beregnet ennå, 0 når ingenting utbetales.';
comment on column ve_ytelse_anvist.beloep is 'Beregnet beløp for perioden. Null når perioden ikke er beregnet ennå.';
comment on column ve_ytelse_anvist.utbetalingsgrad is 'Utbetalingsgrad i prosent. Null når perioden ikke er beregnet ennå.';
comment on column ve_ytelse_anvist.antall_barn is 'Antall barn som gir rett til barnetillegg';
comment on column ve_ytelse_anvist.dagsats_barnetillegg is 'Dagsats for barnetillegg';

create index idx_ve_ytelse_anvist_vedtak on ve_ytelse_anvist (vedtak_ytelse_id);
