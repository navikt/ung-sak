
alter table BR_BEREGNINGSRESULTAT alter column regel_input TYPE oid using (regel_input::oid)
                                , alter column regel_sporing TYPE oid using (regel_sporing::oid);

alter table br_feriepenger alter column feriepenger_regel_input TYPE oid using (feriepenger_regel_input::oid)
                                , alter column feriepenger_regel_sporing TYPE oid using (feriepenger_regel_sporing::oid);
                                
alter table BR_BEREGNINGSRESULTAT add column feriepenger_regel_input oid
                                , add column feriepenger_regel_sporing oid;


