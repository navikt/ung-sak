update br_andel a 
 set beregningsresultat_id=b.BEREGNINGSRESULTAT_FP_ID,
     periode = daterange(b.br_periode_fom, b.br_periode_tom, '[]')
from br_periode b 
  where a.br_periode_id=b.id
;

update br_andel a
   set feriepenger_beloep = b.aarsbeloep
from BR_FERIEPENGER_PR_AAR b
   where a.id=b.beregningsresultat_andel_id
;