create sequence seq_aktivitet_resultat_holder increment by 50 minvalue 1000000;
create sequence seq_aktivitet_resultat_periode increment by 50 minvalue 1000000;

create table aktivitet_resultat_holder
(
    id            bigint                                 not null primary key,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table aktivitet_resultat_holder is 'Aggregat som samler saksbehandlers periodiserte vurderinger av aktivitetsvilkåret.';

create table aktivitet_resultat_periode
(
    id                         bigint                                               not null primary key,
    aktivitet_resultat_holder_id bigint references aktivitet_resultat_holder (id)      not null,
    periode                    daterange                                            not null,
    godkjent                   boolean                                              not null,
    ikke_oppfylt_aarsak        varchar(100),
    manuell_vurdering          boolean                                              not null,
    begrunnelse                text,
    fritekst_vurdering_brev    text,
    vurdert_av                 varchar(100)                                         not null,
    vurdert_tidspunkt          timestamp(3)                                         not null,
    opprettet_av               varchar(20)  default 'VL'                            not null,
    opprettet_tid              timestamp(3) default current_timestamp               not null,
    endret_av                  varchar(20),
    endret_tid                 timestamp(3)
);

comment on table aktivitet_resultat_periode is 'Periodisert vurdering av aktivitetsvilkåret. godkjent=false krever ikke_oppfylt_aarsak.';

create index idx_aktivitet_resultat_periode_resultat_holder on aktivitet_resultat_periode (aktivitet_resultat_holder_id);

alter table gr_akt_inngangsvilkaar_res add column aktivitet_resultat_holder_id bigint references aktivitet_resultat_holder (id);
