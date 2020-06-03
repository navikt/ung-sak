alter table behandling_vedtak_varsel
add column if not exists redusert_utbetaling_aarsaker text;

comment on column behandling_vedtak_varsel.redusert_utbetaling_aarsaker is 'liste med årsaker til ugunst, bestående av enumverdier';

