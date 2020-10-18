update br_andel a 
 set beregningsresultat_id=b.BEREGNINGSRESULTAT_FP_ID,
     periode = daterange(b.br_periode_fom, b.br_periode_tom, '[]')
from br_periode b 
  where a.br_periode_id=b.id
    and a.periode is null and a.beregningsresultat_id is null
    and b.br_periode_fom is not null and b.br_periode_tom is not null
;
