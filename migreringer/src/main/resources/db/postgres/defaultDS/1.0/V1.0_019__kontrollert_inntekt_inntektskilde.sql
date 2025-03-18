alter table if exists kontrollert_inntekt_periode
add column kilde varchar(100) not null;
comment on column kontrollert_inntekt_periode.kilde is 'Kilde for inntekten.';
