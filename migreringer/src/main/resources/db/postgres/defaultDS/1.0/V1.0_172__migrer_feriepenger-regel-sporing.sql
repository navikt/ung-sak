

update br_beregningsresultat res
  set feriepenger_regel_input = null,
      feriepenger_regel_sporing = null
      where feriepenger_regel_input is not null or feriepenger_regel_sporing is not null
  ;
  
alter table BR_FERIEPENGER_PR_AAR ALTER  column br_feriepenger_id DROP NOT NULL;