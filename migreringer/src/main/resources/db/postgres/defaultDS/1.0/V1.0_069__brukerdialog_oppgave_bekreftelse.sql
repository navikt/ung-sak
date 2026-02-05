alter table BD_OPPGAVE ADD COLUMN bekreftelse jsonb;
comment on column BD_OPPGAVE.bekreftelse is 'Json-data med bekreftelsesdetaljer.';
