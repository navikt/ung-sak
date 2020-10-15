alter table br_andel add column fom date,
                     add column tom date,
                     add column feriepenger_beloep bigint,
                     add column beregningsresultat_id bigint;
                     
alter table br_andel add constraint chk_br_periode check ((fom is null and tom is null) OR fom <= tom);

create index br_andel_02 on br_andel ( beregningsresultat_id);
create index br_andel_03 on br_andel ( fom);
create index br_andel_04 on br_andel ( tom);

alter table br_andel add constraint FK_BR_ANDEL_2 foreign key (beregningsresultat_id) references BR_BEREGNINGSRESULTAT(ID);