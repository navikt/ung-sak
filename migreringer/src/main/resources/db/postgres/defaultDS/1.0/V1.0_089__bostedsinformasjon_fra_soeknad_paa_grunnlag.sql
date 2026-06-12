comment on table bosatt_periode_avklaring is 'Resultat etter avklaring av bostedsvilkåret per vilkårsperiode.';


alter table gr_bosatt_avklaring
    rename column foreslatt_avklaring_holder_id to foreslatt_holder_id;

alter table bosatt_periode_avklaring
    add column periode daterange;

update bosatt_periode_avklaring
set periode = daterange(skaeringstidspunkt, (skaeringstidspunkt + interval '1 year')::date, '[)');

alter table bosatt_periode_avklaring
    alter column periode set not null,
    drop column skaeringstidspunkt;

alter table gr_bosatt_avklaring
    add column bosatt_soeknad_grunnlag_id bigint references bosatt_soeknad_grunnlag (id),
    add column resultat_holder_id bigint references bosatt_avklaring_holder (id);

--  For å kunne lagre bostedsgrunnlag med søknadsopplysninger, for deretter å opprette avklaring i senere steg
alter table gr_bosatt_avklaring
    alter column foreslatt_holder_id drop not null;

create index idx_gr_bosatt_avklaring_soeknad_grunnlag on gr_bosatt_avklaring (bosatt_soeknad_grunnlag_id);

drop index idx_bosatt_soeknad_grunnlag_behandling;

alter table bosatt_soeknad_grunnlag
    drop column behandling_id;

