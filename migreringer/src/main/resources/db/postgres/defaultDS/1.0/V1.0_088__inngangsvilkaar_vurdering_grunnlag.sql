create sequence seq_bistand_resultat_holder increment by 50 minvalue 1000000;
create sequence seq_bistand_resultat_periode increment by 50 minvalue 1000000;
create sequence seq_livsopphold_resultat_holder increment by 50 minvalue 1000000;
create sequence seq_livsopphold_resultat_periode increment by 50 minvalue 1000000;
create sequence seq_gr_akt_inngangsvilkaar_res increment by 50 minvalue 1000000;

create table bistand_resultat_holder
(
    id            bigint                                 not null primary key,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table bistand_resultat_holder is 'Aggregat som samler saksbehandlers periodiserte vurderinger av bistandsvilkåret.';

create table bistand_resultat_periode
(
    id                         bigint                                               not null primary key,
    bistand_resultat_holder_id bigint references bistand_resultat_holder (id)      not null,
    periode                    daterange                                            not null,
    godkjent                   boolean                                              not null,
    avslagsarsak               varchar(100),
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

comment on table bistand_resultat_periode is 'Periodisert vurdering av bistandsvilkåret. godkjent=false krever avslagsarsak.';

create index idx_bistand_resultat_periode_resultat_holder on bistand_resultat_periode (bistand_resultat_holder_id);

create table livsopphold_resultat_holder
(
    id            bigint                                 not null primary key,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table livsopphold_resultat_holder is 'Aggregat som samler saksbehandlers periodiserte vurderinger av andre livsoppholdsytelser-vilkåret.';

create table livsopphold_resultat_periode
(
    id                             bigint                                                     not null primary key,
    livsopphold_resultat_holder_id bigint references livsopphold_resultat_holder (id)         not null,
    periode                        daterange                                                  not null,
    godkjent                       boolean                                                    not null,
    avslagsarsak                   varchar(100),
    manuell_vurdering              boolean                                                    not null,
    begrunnelse                    text,
    fritekst_vurdering_brev        text,
    vurdert_av                     varchar(100)                                               not null,
    vurdert_tidspunkt              timestamp(3)                                               not null,
    opprettet_av                   varchar(20)  default 'VL'                                  not null,
    opprettet_tid                  timestamp(3) default current_timestamp                     not null,
    endret_av                      varchar(20),
    endret_tid                     timestamp(3)
);

comment on table livsopphold_resultat_periode is 'Periodisert vurdering av andre livsoppholdsytelser-vilkåret. godkjent=false krever avslagsarsak.';

create index idx_livsopphold_resultat_periode_resultat_holder on livsopphold_resultat_periode (livsopphold_resultat_holder_id);

create table gr_akt_inngangsvilkaar_res
(
    id                           bigint                                              not null primary key,
    behandling_id                bigint references behandling (id)                   not null,
    bistand_resultat_holder_id   bigint references bistand_resultat_holder (id),
    livsopphold_resultat_holder_id bigint references livsopphold_resultat_holder (id),
    aktiv                        boolean      default true                           not null,
    versjon                      bigint       default 0                              not null,
    opprettet_av                 varchar(20)  default 'VL'                           not null,
    opprettet_tid                timestamp(3) default current_timestamp              not null,
    endret_av                    varchar(20),
    endret_tid                   timestamp(3)
);

comment on table gr_akt_inngangsvilkaar_res is 'Grunnlag som knytter en behandling til saksbehandlers vurderinger av bistandsvilkåret og andre livsoppholdsytelser-vilkåret. Kun én aktiv rad per behandling.';

create index idx_gr_akt_inngangsvilkaar_res_behandling on gr_akt_inngangsvilkaar_res (behandling_id);
create unique index uidx_gr_akt_inngangsvilkaar_res_aktiv on gr_akt_inngangsvilkaar_res (behandling_id) where (aktiv = true);
