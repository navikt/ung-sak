alter table if exists kontrollert_inntekt_periode
add column inntekt numeric;
comment on column kontrollert_inntekt_periode.inntekt is 'Kontrollert inntekt for perioden.';

update kontrollert_inntekt_periode set inntekt = COALESCE (ytelse, 0) + COALESCE (arbeidsinntekt, 0);

alter table if exists kontrollert_inntekt_periode
alter column inntekt set not null,
drop column ytelse,
drop column arbeidsinntekt;



