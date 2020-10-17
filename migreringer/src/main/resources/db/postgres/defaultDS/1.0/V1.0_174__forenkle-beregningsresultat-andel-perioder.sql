alter table br_andel add column periode daterange,
                     add column feriepenger_beloep bigint,
                     add column beregningsresultat_id bigint;
                   
-- splitt i to partial constraints da gist ikke har operatorer for boolean datatype  
alter table br_andel add constraint br_andel_ikke_overlapp_periode_ag EXCLUDE USING GIST (
        arbeidsgiver_orgnr WITH =,
        arbeidsforhold_intern_id WITH =,
        br_periode_id WITH =,
        beregningsresultat_id WITH =,
        arbeidsgiver_aktor_id WITH =,
        inntektskategori WITH =,
        arbeidsforhold_type WITH =,
        aktivitet_status WITH =,
        periode WITH &&
    ) where (bruker_er_mottaker=true);  
    
alter table br_andel add constraint br_andel_ikke_overlapp_periode_bruker EXCLUDE USING GIST (
        arbeidsgiver_orgnr WITH =,
        arbeidsforhold_intern_id WITH =,
        br_periode_id WITH =,
        beregningsresultat_id WITH =,
        arbeidsgiver_aktor_id WITH =,
        inntektskategori WITH =,
        arbeidsforhold_type WITH =,
        aktivitet_status WITH =,
        periode WITH &&
    ) where (bruker_er_mottaker=false);  
    
-- sjekk perioder ikke går ut over et år 
alter table br_andel add constraint chk_br_andel_samme_aar check (periode is null OR extract(YEAR from lower(periode)) = extract (YEAR from upper(periode)));

create index br_andel_02 on br_andel ( beregningsresultat_id);

alter table br_andel add constraint FK_BR_ANDEL_2 foreign key (beregningsresultat_id) references BR_BEREGNINGSRESULTAT(ID);