alter table GR_OMP_AKTIVITET add column fravaer_id_fra_soeknad bigint null;
alter table GR_OMP_AKTIVITET add constraint FK_GR_OMP_AKTIVITET_03 foreign key (fravaer_id_fra_soeknad) references OMP_OPPGITT_FRAVAER;
alter table GR_OMP_AKTIVITET alter column fravaer_id drop not null;

alter table OMP_OPPGITT_FRAVAER_PERIODE add column JOURNALPOST_ID VARCHAR(20);
create  index IDX_OMP_OPPGITT_FRAVAER_PERIODE_02 on OMP_OPPGITT_FRAVAER_PERIODE (JOURNALPOST_ID);
