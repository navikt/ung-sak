alter table aktivitet_resultat_periode
    add column regel_input text,
    add column regel_evaluering text;

comment on column aktivitet_resultat_periode.regel_input is 'Input til automatiseringen som ga vurderingen, som JSON. Null når perioden er vurdert manuelt, eller når automatiseringen ikke skriver sporing.';
comment on column aktivitet_resultat_periode.regel_evaluering is 'Utfallet av automatiseringen. Null når perioden er vurdert manuelt, eller når automatiseringen ikke skriver sporing.';

alter table bistand_resultat_periode
    add column regel_input text,
    add column regel_evaluering text;

comment on column bistand_resultat_periode.regel_input is 'Input til automatiseringen som ga vurderingen, som JSON. Null når perioden er vurdert manuelt, eller når automatiseringen ikke skriver sporing.';
comment on column bistand_resultat_periode.regel_evaluering is 'Utfallet av automatiseringen. Null når perioden er vurdert manuelt, eller når automatiseringen ikke skriver sporing.';

alter table bosted_resultat_periode
    add column regel_input text,
    add column regel_evaluering text;

comment on column bosted_resultat_periode.regel_input is 'Input til automatiseringen som ga vurderingen, som JSON. Null når perioden er vurdert manuelt, eller når automatiseringen ikke skriver sporing.';
comment on column bosted_resultat_periode.regel_evaluering is 'Utfallet av automatiseringen. Null når perioden er vurdert manuelt, eller når automatiseringen ikke skriver sporing.';

alter table livsopphold_resultat_periode
    add column regel_input text,
    add column regel_evaluering text;

comment on column livsopphold_resultat_periode.regel_input is 'Input til automatiseringen som ga vurderingen, som JSON. Null når perioden er vurdert manuelt, eller når automatiseringen ikke skriver sporing.';
comment on column livsopphold_resultat_periode.regel_evaluering is 'Utfallet av automatiseringen. Null når perioden er vurdert manuelt, eller når automatiseringen ikke skriver sporing.';
