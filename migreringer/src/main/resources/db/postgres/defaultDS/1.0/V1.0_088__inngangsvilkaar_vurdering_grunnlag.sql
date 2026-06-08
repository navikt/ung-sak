create sequence seq_bistand_vurd_holder increment by 50 minvalue 1000000;
create sequence seq_bistand_vurd_periode increment by 50 minvalue 1000000;
create sequence seq_livsopphold_vurd_holder increment by 50 minvalue 1000000;
create sequence seq_livsopphold_vurd_periode increment by 50 minvalue 1000000;
create sequence seq_gr_vurdering increment by 50 minvalue 1000000;

create table bistand_vurd_holder
(
    id            bigint                                 not null primary key,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table bistand_vurd_holder is 'Aggregat som samler saksbehandlers periodiserte vurderinger av bistandsvilkåret.';

create table bistand_vurd_periode
(
    id                      bigint                                             not null primary key,
    bistand_vurd_holder_id  bigint references bistand_vurd_holder (id)        not null,
    periode                 daterange                                          not null,
    godkjent                boolean                                            not null,
    avslagsarsak            varchar(100),
    vurdert_av              varchar(100)                                       not null,
    vurdert_tidspunkt       timestamp(3)                                       not null,
    opprettet_av            varchar(20)  default 'VL'                          not null,
    opprettet_tid           timestamp(3) default current_timestamp             not null,
    endret_av               varchar(20),
    endret_tid              timestamp(3)
);

comment on table bistand_vurd_periode is 'Periodisert vurdering av bistandsvilkåret. godkjent=false krever avslagsarsak.';

create index idx_bistand_vurd_periode_holder on bistand_vurd_periode (bistand_vurd_holder_id);

create table livsopphold_vurd_holder
(
    id            bigint                                 not null primary key,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table livsopphold_vurd_holder is 'Aggregat som samler saksbehandlers periodiserte vurderinger av andre livsoppholdsytelser-vilkåret.';

create table livsopphold_vurd_periode
(
    id                          bigint                                                  not null primary key,
    livsopphold_vurd_holder_id  bigint references livsopphold_vurd_holder (id)         not null,
    periode                     daterange                                               not null,
    godkjent                    boolean                                                 not null,
    avslagsarsak                varchar(100),
    vurdert_av                  varchar(100)                                            not null,
    vurdert_tidspunkt           timestamp(3)                                            not null,
    opprettet_av                varchar(20)  default 'VL'                               not null,
    opprettet_tid               timestamp(3) default current_timestamp                  not null,
    endret_av                   varchar(20),
    endret_tid                  timestamp(3)
);

comment on table livsopphold_vurd_periode is 'Periodisert vurdering av andre livsoppholdsytelser-vilkåret. godkjent=false krever avslagsarsak.';

create index idx_livsopphold_vurd_periode_holder on livsopphold_vurd_periode (livsopphold_vurd_holder_id);

create table gr_vurdering
(
    id                         bigint                                           not null primary key,
    behandling_id              bigint references behandling (id)                not null,
    bistand_vurd_holder_id     bigint references bistand_vurd_holder (id),
    livsopphold_vurd_holder_id bigint references livsopphold_vurd_holder (id),
    aktiv                      boolean      default true                        not null,
    versjon                    bigint       default 0                           not null,
    opprettet_av               varchar(20)  default 'VL'                        not null,
    opprettet_tid              timestamp(3) default current_timestamp           not null,
    endret_av                  varchar(20),
    endret_tid                 timestamp(3)
);

comment on table gr_vurdering is 'Grunnlag som knytter en behandling til saksbehandlers vurderinger av bistandsvilkåret og andre livsoppholdsytelser-vilkåret. Kun én aktiv rad per behandling.';

create index idx_gr_vurdering_behandling on gr_vurdering (behandling_id);
create unique index uidx_gr_vurdering_aktiv on gr_vurdering (behandling_id) where (aktiv = true);
