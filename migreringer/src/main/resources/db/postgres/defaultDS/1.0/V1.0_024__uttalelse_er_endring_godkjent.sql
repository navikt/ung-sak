alter table uttalelse add er_endring_godkjent boolean;

comment on column uttalelse.er_endring_godkjent is 'Angir om endringen er godkjent av deltager';
