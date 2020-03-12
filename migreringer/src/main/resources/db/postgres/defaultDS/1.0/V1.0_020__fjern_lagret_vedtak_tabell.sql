drop table lagret_vedtak cascade;

alter table behandling_resultat add column sendt_varsel_om_revurdering boolean;

alter table behandling_resultat rename to vedtak_varsel;

alter sequence SEQ_BEHANDLING_RESULTAT rename to SEQ_VEDTAK_VARSEL;

