alter table gr_bosatt_avklaring
    rename column bosatt_avklaring_holder_id to foreslatt_avklaring_holder_id;

alter table gr_bosatt_avklaring
    add column fastsatt_avklaring_holder_id bigint references bosatt_avklaring_holder (id);

comment on column gr_bosatt_avklaring.foreslatt_avklaring_holder_id is 'Saksbehandlers foreslåtte bostedsavklaringer per skjæringstidspunkt.';
comment on column gr_bosatt_avklaring.fastsatt_avklaring_holder_id is 'Fastsatte bostedsavklaringer – settes automatisk ved utløpt/nei-uttalelse eller etter saksbehandlers re-vurdering. Brukes av automatisk vilkårsvurdering.';
