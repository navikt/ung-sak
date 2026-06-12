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

-- bostedsvilkår vurderingsresultat

create sequence seq_bosted_resultat_holder increment by 50 minvalue 1000000;
create sequence seq_bosted_resultat_periode increment by 50 minvalue 1000000;

create table bosted_resultat_holder
(
    id            bigint                                 not null primary key,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table bosted_resultat_holder is 'Aggregat som samler saksbehandlers periodiserte vurderinger av bostedsvilkåret.';

create table bosted_resultat_periode
(
    id                       bigint                                          not null primary key,
    bosted_resultat_holder_id bigint references bosted_resultat_holder (id) not null,
    periode                  daterange                                       not null,
    godkjent                 boolean                                         not null,
    ikke_oppfylt_aarsak      varchar(100),
    manuell_vurdering        boolean                                         not null,
    begrunnelse              text,
    fritekst_vurdering_brev  text,
    vurdert_av               varchar(100)                                    not null,
    vurdert_tidspunkt        timestamp(3)                                    not null,
    opprettet_av             varchar(20)  default 'VL'                       not null,
    opprettet_tid            timestamp(3) default current_timestamp          not null,
    endret_av                varchar(20),
    endret_tid               timestamp(3)
);

comment on table bosted_resultat_periode is 'Periodisert vurdering av bostedsvilkåret. godkjent=false krever ikke_oppfylt_aarsak.';

create index idx_bosted_resultat_periode_resultat_holder on bosted_resultat_periode (bosted_resultat_holder_id);

alter table gr_akt_inngangsvilkaar_res
    add column bosted_resultat_holder_id bigint references bosted_resultat_holder (id);

