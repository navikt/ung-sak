update br_andel a
   set feriepenger_beloep = b.aarsbeloep
from BR_FERIEPENGER_PR_AAR b
   where a.id=b.beregningsresultat_andel_id
     and a.feriepenger_beloep is null and b.aarsbeloep is not null
;
