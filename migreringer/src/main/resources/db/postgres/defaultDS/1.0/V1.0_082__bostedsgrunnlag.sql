create sequence seq_bosatt_avklaring_holder increment by 50 minvalue 1000000;
create sequence seq_gr_bosatt_avklaring increment by 50 minvalue 1000000;
create sequence seq_bosatt_periode_avklaring increment by 50 minvalue 1000000;
create sequence seq_bosatt_soeknad_grunnlag increment by 50 minvalue 1000000;
create sequence seq_bosatt_informasjon_fra_soeknad increment by 50 minvalue 1000000;

create table bosatt_avklaring_holder
(
    id            bigint                                 not null primary key,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table bosatt_avklaring_holder is 'Aggregat som samler alle bostedsavklaringer. Kan deles mellom behandlinger ved revurdering uten endringer.';

create table bosatt_periode_avklaring
(
    id                         bigint                                          not null primary key,
    bosatt_avklaring_holder_id bigint references bosatt_avklaring_holder (id) not null,
    referanse                  uuid                                            not null unique,
    skaeringstidspunkt         date                                            not null,
    er_bosatt_i_trondheim      boolean                                         not null,
    fraflyttings_dato          date,
    fraflyttings_aarsak        varchar(100),
    versjon                    bigint       default 0                          not null,
    opprettet_av               varchar(20)  default 'VL'                      not null,
    opprettet_tid              timestamp(3) default current_timestamp          not null,
    endret_av                  varchar(20),
    endret_tid                 timestamp(3)
);

comment on table bosatt_periode_avklaring is 'Avklaring av bostedsvilkåret per vilkårsperiode. Brukes som referanse i etterlysning og uttalelse.';

create index idx_bosatt_periode_avklaring_holder on bosatt_periode_avklaring (bosatt_avklaring_holder_id);

create table gr_bosatt_avklaring
(
    id                            bigint                                           not null primary key,
    behandling_id                 bigint references behandling (id)                not null,
    foreslatt_avklaring_holder_id bigint references bosatt_avklaring_holder (id)  not null,
    fastsatt_avklaring_holder_id  bigint references bosatt_avklaring_holder (id),
    grunnlag_ref                  uuid                                             not null,
    aktiv                         boolean      default true                        not null,
    versjon                       bigint       default 0                           not null,
    opprettet_av                  varchar(20)  default 'VL'                        not null,
    opprettet_tid                 timestamp(3) default current_timestamp            not null,
    endret_av                     varchar(20),
    endret_tid                    timestamp(3)
);

comment on table gr_bosatt_avklaring is 'Grunnlag som kobler en behandling til bostedsavklarings-aggregatet. Grunnlag_ref brukes som nøkkel i etterlysning-tabellen.';
comment on column gr_bosatt_avklaring.foreslatt_avklaring_holder_id is 'Saksbehandlers foreslåtte bostedsavklaringer per fom-dato.';
comment on column gr_bosatt_avklaring.fastsatt_avklaring_holder_id is 'Fastsatte bostedsavklaringer – settes automatisk ved utløpt/nei-uttalelse eller etter saksbehandlers re-vurdering. Brukes av automatisk vilkårsvurdering.';

create index idx_gr_bosatt_avklaring_behandling on gr_bosatt_avklaring (behandling_id);

create table bosatt_soeknad_grunnlag
(
    id            bigint                                 not null primary key,
    behandling_id bigint references behandling (id)     not null unique,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table bosatt_soeknad_grunnlag is 'Søknadsbaserte bostedsopplysninger per behandling. Lagres additivt uten deaktivering.';

create index idx_bosatt_soeknad_grunnlag_behandling on bosatt_soeknad_grunnlag (behandling_id);

create table bosatt_informasjon_fra_soeknad
(
    id                         bigint                                          not null primary key,
    bosatt_soeknad_grunnlag_id bigint references bosatt_soeknad_grunnlag (id) not null,
    journalpost_id             varchar(50)                                     not null,
    fom_dato                   date                                            not null,
    er_bosatt_i_trondheim      boolean                                         not null,
    opprettet_av               varchar(20)  default 'VL'                       not null,
    opprettet_tid              timestamp(3) default current_timestamp           not null,
    endret_av                  varchar(20),
    endret_tid                 timestamp(3)
);

comment on table bosatt_informasjon_fra_soeknad is 'Bostedsopplysning oppgitt av bruker i søknaden. Koblet til bosatt_soeknad_grunnlag.';

create index idx_bosatt_informasjon_grunnlag on bosatt_informasjon_fra_soeknad (bosatt_soeknad_grunnlag_id);
