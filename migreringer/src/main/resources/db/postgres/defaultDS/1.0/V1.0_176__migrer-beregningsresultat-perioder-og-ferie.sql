alter table br_andel drop constraint if exists chk_br_andel_samme_aar  ;
-- upper gir dato etter tom (alltid exclusive i db)
alter table br_andel add constraint chk_br_andel_samme_aar check (periode is null OR date_part('year'::text, lower(periode)) = date_part('year'::text, upper(periode) - interval '1 day'));

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
