alter table ung_ungdomsprogram_utvidet_kvote rename to ung_ungdomsprogram_forlenget_periode;

alter table ung_ungdomsprogram_forlenget_periode rename column har_utvidet_kvote to har_forlenget_periode;

alter sequence seq_ung_ungdomsprogram_utvidet_kvote_id rename to seq_ung_ungdomsprogram_forlenget_periode_id;

alter table ung_gr_ungdomsprogramperiode
    rename column ung_ungdomsprogramp_utvidet_kvote_id to ung_ungdomsprogramp_forlenget_periode_id;

drop index if exists idx_ung_gr_ungdomsprogramperiode_utvidet_kvote;
create index idx_ung_gr_ungdomsprogramperiode_forlenget_periode
    on ung_gr_ungdomsprogramperiode (ung_ungdomsprogramp_forlenget_periode_id);

comment on table ung_ungdomsprogram_forlenget_periode is
    'Angir om bruker har forlenget periode i ungdomsprogrammet (300 virkedager i stedet for 260).';
comment on column ung_ungdomsprogram_forlenget_periode.hjemmel is
    'Lovhjemmel for forlenget periode. Programmet kan i særlige tilfeller forlenges med inntil 8 uker dersom Arbeids- og velferdsetaten vurderer at ytterligere deltakelse i programmet vil ha avgjørende betydning for personens mulighet for å komme i ordinært arbeid eller ordinær utdanning (§ 6).';

