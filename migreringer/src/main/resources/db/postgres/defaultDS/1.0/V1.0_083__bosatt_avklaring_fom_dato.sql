alter table bosatt_avklaring
    rename column skjaeringstidspunkt to fom_dato;

comment on column bosatt_avklaring.fom_dato is 'Fom-dato for avklaringsperioden. For en periode uten fraflytting er dette vilkårsperiodens fom-dato.';
