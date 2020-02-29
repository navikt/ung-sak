alter table BEHANDLING_VEDTAK add column behandling_id BIGINT NOT NULL;

create unique index UIDX_BEHANDLING_VEDTAK_02 on behandling_vedtak (behandling_id);

update behandling_vedtak bv set behandling_id = (select behandling_id from behandling_resultat br where br.id=bv.behandling_resultat_id);

alter table behandling_vedtak drop column behandling_resultat_id;