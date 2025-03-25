alter table uttalelse add har_godtatt_endringen boolean;

comment on column uttalelse.har_godtatt_endringen is 'Angir om endringen er godkjent av deltager';
