alter table if exists SAK_INFOTRYGD_MIGRERING add column if not exists aktiv BOOLEAN default true;
UPDATE SAK_INFOTRYGD_MIGRERING SET aktiv = true where aktiv is null;
alter table if exists SAK_INFOTRYGD_MIGRERING alter column aktiv set NOT NULL;
