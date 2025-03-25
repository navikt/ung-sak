alter table uttalelse add har_godtatt_endringen boolean not null;
alter table uttalelse alter column uttalelse drop not null;



comment on column uttalelse.har_godtatt_endringen is 'Angir om endringen er godkjent av deltager';
