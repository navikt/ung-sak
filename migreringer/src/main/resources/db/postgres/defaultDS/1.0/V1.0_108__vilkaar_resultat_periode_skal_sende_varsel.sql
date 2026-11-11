alter table bosted_resultat_periode add column skal_sende_varsel boolean not null default true;
alter table bistand_resultat_periode add column skal_sende_varsel boolean not null default true;
alter table aktivitet_resultat_periode add column skal_sende_varsel boolean not null default true;
alter table livsopphold_resultat_periode add column skal_sende_varsel boolean not null default true;
