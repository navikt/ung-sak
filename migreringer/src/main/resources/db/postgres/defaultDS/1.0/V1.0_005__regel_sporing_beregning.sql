alter table UNG_SATS_PERIODER add column regel_input oid;
alter table UNG_SATS_PERIODER add column regel_sporing oid;

update UNG_SATS_PERIODER set regel_input = lo_from_bytea(0, 'regelinput'::bytea) where regel_input is null;
update UNG_SATS_PERIODER set regel_sporing = lo_from_bytea(0, 'regelsporing'::bytea) where regel_sporing is null;

alter table UNG_SATS_PERIODER alter column regel_input set not null;
alter table UNG_SATS_PERIODER alter column regel_sporing set not null;
