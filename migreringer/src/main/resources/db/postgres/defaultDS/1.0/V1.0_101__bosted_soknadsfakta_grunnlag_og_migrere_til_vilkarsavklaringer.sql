-- gr_bosatt_avklaring blandet to akser i samme rad: avklaringene (som skal over i gr_vilkaar_avklaring) og
-- bostedsinformasjonen fra søknaden.
create sequence seq_gr_bosted_soknadsfakta increment by 50 minvalue 1000000;

create table gr_bosted_soknadsfakta
(
    id                                   bigint primary key,
    behandling_id                        bigint       not null references behandling (id),
    bostedsinformasjon_soeknad_holder_id bigint       not null references bostedsinformasjon_soeknad_holder (id),
    aktiv                                boolean      not null default true,
    versjon                              bigint       not null default 0,
    opprettet_av                         varchar(20)  not null default 'VL',
    opprettet_tid                        timestamp(3) not null default current_timestamp,
    endret_av                            varchar(20),
    endret_tid                           timestamp(3)
);

comment on table gr_bosted_soknadsfakta is
    'Grunnlag som kobler en behandling til bostedsopplysningene oppgitt i søknaden. Én aktiv rad per behandling.';

create unique index uidx_gr_bosted_soknadsfakta_aktiv on gr_bosted_soknadsfakta (behandling_id) where aktiv;
create index idx_gr_bosted_soknadsfakta_behandling on gr_bosted_soknadsfakta (behandling_id);
create index idx_gr_bosted_soknadsfakta_holder on gr_bosted_soknadsfakta (bostedsinformasjon_soeknad_holder_id);
