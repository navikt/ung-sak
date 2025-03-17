create table kontrollert_inntekt_perioder
(
    id                bigint not null primary key,
    behandling_id     bigint not null references behandling(id),
    aktiv             boolean not null,
    opprettet_tid     timestamp default CURRENT_TIMESTAMP not null,
    opprettet_av         varchar(20) not null default 'VL',
    endret_av         varchar(20),
    endret_tid        timestamp
);

create index idx_kontrollert_inntekt_perioder_behandling_id
    on kontrollert_inntekt_perioder (behandling_id);

create unique index idx_kontrollert_inntekt_perioder_aktiv_behandling_id
    on kontrollert_inntekt_perioder (behandling_id)
    where aktiv = true;

create sequence if not exists seq_kontrollert_inntekt_perioder increment by 50 minvalue 1000000;


comment on table kontrollert_inntekt_perioder is 'Inneholder kontrollerte perioder for inntekt.';
comment on column kontrollert_inntekt_perioder.id is 'Primary Key. Unik identifikator for kontroll av inntekt.';
comment on column kontrollert_inntekt_perioder.behandling_id is 'Referanse til behandling.';
comment on column kontrollert_inntekt_perioder.aktiv is 'Angir om aggregatet er aktivt.';
comment on column kontrollert_inntekt_perioder.opprettet_tid is 'Tidspunkt for når ytelsen ble opprettet.';
comment on column kontrollert_inntekt_perioder.endret_tid is 'Tidspunkt for når ytelsen sist ble endret.';


create table kontrollert_inntekt_periode
(
    id                bigint not null primary key,
    kontrollert_inntekt_perioder_id bigint not null references kontrollert_inntekt_perioder(id),
    periode           daterange not null,
    arbeidsinntekt   numeric,
    ytelse   numeric,
    opprettet_tid     timestamp default CURRENT_TIMESTAMP not null,
    opprettet_av         varchar(20) not null default 'VL',
    endret_av         varchar(20),
    endret_tid        timestamp
);

create index idx_kontrollert_inntekt_periode_kontrollert_inntekt_perioder_id on kontrollert_inntekt_periode (kontrollert_inntekt_perioder_id);

alter table kontrollert_inntekt_periode
    add constraint no_overlapping_kontrollerte_perioder
    exclude using gist (kontrollert_inntekt_perioder_id with =, periode with &&);

create sequence if not exists seq_kontrollert_inntekt_periode increment by 50 minvalue 1000000;

comment on table kontrollert_inntekt_periode is 'Inneholder perioder for kontroll av inntekt.';
comment on column kontrollert_inntekt_periode.id is 'Primary Key. Unik identifikator for perioden.';
comment on column kontrollert_inntekt_periode.kontrollert_inntekt_perioder_id is 'Referanse til kontrollerte inntekt perioder.';
comment on column kontrollert_inntekt_periode.periode is 'Periode for ytelsen som daterange.';
comment on column kontrollert_inntekt_periode.arbeidsinntekt is 'Arbeidsinntekt for perioden.';
comment on column kontrollert_inntekt_periode.ytelse is 'Ytelse for perioden.';
comment on column kontrollert_inntekt_periode.opprettet_tid is 'Tidspunkt for når perioden ble opprettet.';
comment on column kontrollert_inntekt_periode.endret_tid is 'Tidspunkt for når perioden sist ble endret.';
