alter table if exists kontrollert_inntekt_periode
add column if not exists manuelt_vurdert_begrunnelse varchar(4000);

comment on column kontrollert_inntekt_periode.manuelt_vurdert_begrunnelse is 'Begrunnelse for valg og inntekt ved manuell vurdering';
