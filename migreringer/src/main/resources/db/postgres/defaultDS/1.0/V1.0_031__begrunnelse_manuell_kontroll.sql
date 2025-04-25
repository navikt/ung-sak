alter table if exists kontrollert_inntekt_periode
add column if not exists begrunnelse_manuell_kontroll varchar(4000);

comment on column kontrollert_inntekt_periode.begrunnelse_manuell_kontroll is 'Begrunnelse for valg og inntekt ved manuell vurdering';
