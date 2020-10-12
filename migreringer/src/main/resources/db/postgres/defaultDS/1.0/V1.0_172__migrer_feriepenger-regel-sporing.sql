

update br_beregningsresultat res
  set feriepenger_regel_input = f.feriepenger_regel_input,
      feriepenger_regel_sporing = f.feriepenger_regel_sporing
      
  from br_feriepenger f 
  where f.beregningsresultat_fp_id = res.id
    and res.feriepenger_regel_input IS NULL
    
  ;