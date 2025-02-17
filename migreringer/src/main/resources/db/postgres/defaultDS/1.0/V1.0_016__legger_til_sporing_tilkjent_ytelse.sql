alter table TILKJENT_YTELSE add column regel_input TEXT;
alter table TILKJENT_YTELSE add column regel_sporing TEXT;

UPDATE TILKJENT_YTELSE SET regel_input = 'regelinput' WHERE regel_input IS NULL;
UPDATE TILKJENT_YTELSE SET regel_sporing = 'regelsporing' WHERE regel_sporing IS NULL;

alter table TILKJENT_YTELSE alter column regel_input set not null;
alter table TILKJENT_YTELSE alter column regel_sporing set not null;
