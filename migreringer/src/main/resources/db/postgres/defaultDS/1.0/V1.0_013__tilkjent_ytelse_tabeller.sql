create table tilkjent_ytelse
(
    id                serial primary key,
    behandling_id     bigint not null references behandling(id),
    aktiv             boolean not null,
    opprettet_tid     timestamp default CURRENT_TIMESTAMP not null,
    opprettet_av         varchar(20),
    endret_av         varchar(20),
    endret_tid        timestamp
);

create index idx_tilkjent_ytelse_behandling_id
    on tilkjent_ytelse (behandling_id);

create unique index idx_tilkjent_ytelse_aktiv_behandling_id
    on tilkjent_ytelse (behandling_id)
    where aktiv = true;

create sequence if not exists seq_tilkjent_ytelse increment by 50 minvalue 1000000;


comment on table tilkjent_ytelse is 'Inneholder tilkjent ytelse.';
comment on column tilkjent_ytelse.id is 'Primary Key. Unik identifikator for tilkjent ytelse.';
comment on column tilkjent_ytelse.behandling_id is 'Referanse til behandling.';
comment on column tilkjent_ytelse.aktiv is 'Angir om ytelsen er aktiv.';
comment on column tilkjent_ytelse.opprettet_tid is 'Tidspunkt for når ytelsen ble opprettet.';
comment on column tilkjent_ytelse.endret_tid is 'Tidspunkt for når ytelsen sist ble endret.';


create table tilkjent_ytelse_periode
(
    id                serial primary key,
    tilkjent_ytelse_id bigint not null references tilkjent_ytelse(id),
    periode           daterange not null,
    uredusert_belop   numeric not null,
    reduksjon         numeric not null,
    redusert_belop    numeric not null,
    dagsats           numeric not null,
    utbetalingsgrad   int not null,
    opprettet_tid     timestamp default CURRENT_TIMESTAMP not null,
    opprettet_av         varchar(20),
    endret_av         varchar(20),
    endret_tid        timestamp
);

create index idx_tilkjent_ytelse_periode_tilkjent_ytelse_id on tilkjent_ytelse_periode (tilkjent_ytelse_id);

alter table tilkjent_ytelse_periode
    add constraint no_overlapping_daterange
    exclude using gist (tilkjent_ytelse_id with =, periode with &&);

create sequence if not exists seq_tilkjent_ytelse_periode increment by 50 minvalue 1000000;

comment on table tilkjent_ytelse_periode is 'Inneholder perioder for tilkjent ytelse.';
comment on column tilkjent_ytelse_periode.id is 'Primary Key. Unik identifikator for perioden.';
comment on column tilkjent_ytelse_periode.tilkjent_ytelse_id is 'Referanse til tilkjent ytelse.';
comment on column tilkjent_ytelse_periode.periode is 'Periode for ytelsen som daterange.';
comment on column tilkjent_ytelse_periode.uredusert_belop is 'Uredusert beløp for perioden.';
comment on column tilkjent_ytelse_periode.reduksjon is 'Reduksjon i beløp for perioden.';
comment on column tilkjent_ytelse_periode.redusert_belop is 'Redusert beløp for perioden.';
comment on column tilkjent_ytelse_periode.dagsats is 'Dagsats for perioden.';
comment on column tilkjent_ytelse_periode.utbetalingsgrad is 'Utbetalingsgrad for perioden.';
comment on column tilkjent_ytelse_periode.opprettet_tid is 'Tidspunkt for når perioden ble opprettet.';
comment on column tilkjent_ytelse_periode.endret_tid is 'Tidspunkt for når perioden sist ble endret.';
