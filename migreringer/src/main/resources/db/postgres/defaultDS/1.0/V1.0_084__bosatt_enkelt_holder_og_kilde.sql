-- fjern fastsatt_avklaring_holder_id fra gr_bosatt_avklaring (enkelt holder)
alter table gr_bosatt_avklaring drop column fastsatt_avklaring_holder_id;

-- legg til kilde på bosatt_periode_avklaring for å spore dataopphav
alter table bosatt_periode_avklaring add column kilde varchar(50) not null default 'SAKSBEHANDLER';
