alter table OLP_VURDERT_OPPLAERING drop column FOM;
alter table OLP_VURDERT_OPPLAERING drop column TOM;
alter table OLP_VURDERT_OPPLAERING drop column institusjon;
alter table OLP_VURDERT_OPPLAERING add column journalpost_id varchar(50) not null;

create unique index UIDX_OLP_VURDERT_OPPLAERING_1 ON OLP_VURDERT_OPPLAERING (journalpost_id, holder_id);
