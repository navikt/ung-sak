alter table uttalelse add har_godtatt_endringen boolean not null;
alter table uttalelse alter column uttalelse drop not null;
alter table uttalelse rename column uttalelse to uttalelse_begrunnelse;

comment on column uttalelse.uttalelse_begrunnelse is 'Begrunnelse for uttalelsen';
comment on column uttalelse.har_godtatt_endringen is 'Angir om endringen er godkjent av deltager';
