alter table br_andel add column periode daterange,
                     add column feriepenger_beloep bigint,
                     add column beregningsresultat_id bigint;
                     
alter table br_andel add constraint br_andel_ikke_overlapp_periode EXCLUDE USING GIST (
        arbeidsgiver_orgnr WITH =,
        arbeidsforhold_intern_id WITH =,
        arbeidsgiver_aktor_id WITH =,
        inntektskategori WITH =,
        arbeidsforhold_type WITH =,
        br_periode_id WITH =,
        beregningsresultat_id WITH =,
        periode WITH &&
    );  
    
-- sjekk perioder ikke går ut over et år 
alter table br_andel add constraint chk_br_andel_samme_aar check (periode is null OR extract(YEAR from lower(periode)) = extract (YEAR from upper(periode)));

create index br_andel_02 on br_andel ( beregningsresultat_id);

alter table br_andel add constraint FK_BR_ANDEL_2 foreign key (beregningsresultat_id) references BR_BEREGNINGSRESULTAT(ID);