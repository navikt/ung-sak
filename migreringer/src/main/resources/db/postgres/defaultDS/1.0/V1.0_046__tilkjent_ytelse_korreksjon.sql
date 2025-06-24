create table korrigert_ytelse_periode
(
    id                              bigint not null primary key,
    tilkjent_ytelse_id              bigint not null references tilkjent_ytelse(id),
    periode                         daterange not null,
    korrigert_dagsats               numeric not null,
    aarsak_for_korrigering          varchar(100) not null,
    opprettet_tid                   timestamp default CURRENT_TIMESTAMP not null,
    opprettet_av                    varchar(20) not null default 'VL',
    endret_av                       varchar(20),
    endret_tid                      timestamp
);

create index idx_korrigert_ytelse_periode_tilkjent_ytelse_id on korrigert_ytelse_periode (tilkjent_ytelse_id);

alter table korrigert_ytelse_periode
    add constraint no_overlapping_daterange
    exclude using gist (tilkjent_ytelse_id with =, periode with &&);

create sequence if not exists seq_korrigert_ytelse_periode increment by 50 minvalue 1000000;

comment on table korrigert_ytelse_periode is 'Inneholder perioder for korrigert tilkjent ytelse som overskriver den beregnede ytelsen.';
comment on column korrigert_ytelse_periode.id is 'Primary Key. Unik identifikator for perioden.';
comment on column korrigert_ytelse_periode.tilkjent_ytelse_id is 'Referanse til tilkjent ytelse.';
comment on column korrigert_ytelse_periode.periode is 'Periode for ytelsen som daterange.';
comment on column korrigert_ytelse_periode.korrigert_dagsats is 'Korrigert dagsats for perioden.';
comment on column korrigert_ytelse_periode.aarsak_for_korrigering is 'Årsak for korrigering fra det som er beregnet.';
comment on column korrigert_ytelse_periode.opprettet_tid is 'Tidspunkt for når perioden ble opprettet.';
comment on column korrigert_ytelse_periode.endret_tid is 'Tidspunkt for når perioden sist ble endret.';
