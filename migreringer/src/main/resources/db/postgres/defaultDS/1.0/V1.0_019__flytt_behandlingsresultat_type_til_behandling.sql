alter table behandling add column BEHANDLING_RESULTAT_TYPE VARCHAR(100) DEFAULT 'IKKE_FASTSATT' NOT NULL;
update behandling b set behandling_resultat_type = (select behandling_resultat_type from behandling_resultat br where br.behandling_id = b.id and br.behandling_resultat_type is not null);
alter table behandling_resultat drop column behandling_resultat_type;
drop table BEHANDLING_RESULTAT_YT_KONSEK cascade;