alter table GR_OMP_AKTIVITET add column fravaer_id_fra_korrigering_im bigint null;
alter table GR_OMP_AKTIVITET add constraint FK_GR_OMP_AKTIVITET_04 foreign key (fravaer_id_fra_korrigering_im) references OMP_OPPGITT_FRAVAER;
