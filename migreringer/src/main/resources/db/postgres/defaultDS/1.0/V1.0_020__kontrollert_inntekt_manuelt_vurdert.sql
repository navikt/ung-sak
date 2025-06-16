alter table if exists kontrollert_inntekt_periode
add column er_manuelt_vurdert boolean not null;
comment on column kontrollert_inntekt_periode.er_manuelt_vurdert is 'Sier om perioden har blitt manuelt vurdert av saksbehandler.';
