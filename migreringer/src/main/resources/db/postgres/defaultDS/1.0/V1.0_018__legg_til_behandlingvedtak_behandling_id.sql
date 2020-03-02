alter table BEHANDLING_VEDTAK add column behandling_id BIGINT NOT NULL;
create unique index UIDX_BEHANDLING_VEDTAK_02 on behandling_vedtak (behandling_id);
update behandling_vedtak bv set behandling_id = (select behandling_id from behandling_resultat br where br.id=bv.behandling_resultat_id);
alter table behandling_vedtak drop column behandling_resultat_id;
alter table behandling_vedtak add constraint FK_BEHANDLING_VEDTAK_01 foreign key (BEHANDLING_ID) references BEHANDLING(ID);

alter table UTTAK_RESULTAT add column behandling_id BIGINT NOT NULL;
create unique index UIDX_UTTAK_RESULTAT_02 on UTTAK_RESULTAT (behandling_id) WHERE (aktiv=true);
update UTTAK_RESULTAT ur set behandling_id = (select behandling_id from behandling_resultat br where br.id=ur.behandling_resultat_id);
alter table UTTAK_RESULTAT drop column behandling_resultat_id;
alter table UTTAK_RESULTAT add constraint FK_UTTAK_RESULTAT_01 foreign key (BEHANDLING_ID) references BEHANDLING(ID);

