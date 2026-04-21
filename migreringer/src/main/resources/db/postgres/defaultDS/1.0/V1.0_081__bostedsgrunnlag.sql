create sequence seq_bosatt_avklaring_holder increment by 50 minvalue 1000000;
create sequence seq_bosatt_avklaring increment by 50 minvalue 1000000;
create sequence seq_gr_bosatt_avklaring increment by 50 minvalue 1000000;

create table bosatt_avklaring_holder
(
    id            bigint                                 not null primary key,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table bosatt_avklaring_holder is 'Aggregat som samler alle bostedsavklaringer. Kan deles mellom behandlinger ved revurdering uten endringer.';

create table bosatt_avklaring
(
    id                           bigint                                           not null primary key,
    bosatt_avklaring_holder_id   bigint references bosatt_avklaring_holder (id)   not null,
    skjaeringstidspunkt          date                                             not null,
    er_bosatt_i_trondheim        boolean                                          not null,
    opprettet_av                 varchar(20)  default 'VL'                        not null,
    opprettet_tid                timestamp(3) default current_timestamp            not null,
    endret_av                    varchar(20),
    endret_tid                   timestamp(3)
);

comment on table bosatt_avklaring is 'Fakta-avklaring om brukers bosted for ett skjæringstidspunkt. Skjæringstidspunkt er fom-datoen i tilhørende vilkårsperiode.';

create index idx_bosatt_avklaring_holder on bosatt_avklaring (bosatt_avklaring_holder_id);

create table gr_bosatt_avklaring
(
    id                           bigint                                           not null primary key,
    behandling_id                bigint references behandling (id)                not null,
    bosatt_avklaring_holder_id   bigint references bosatt_avklaring_holder (id)   not null,
    grunnlag_ref                 uuid                                             not null,
    aktiv                        boolean      default true                        not null,
    versjon                      bigint       default 0                           not null,
    opprettet_av                 varchar(20)  default 'VL'                        not null,
    opprettet_tid                timestamp(3) default current_timestamp            not null,
    endret_av                    varchar(20),
    endret_tid                   timestamp(3)
);

comment on table gr_bosatt_avklaring is 'Grunnlag som kobler en behandling til bostedsavklarings-aggregatet. Grunnlag_ref brukes som nøkkel i etterlysning-tabellen.';

create index idx_gr_bosatt_avklaring_behandling on gr_bosatt_avklaring (behandling_id);
