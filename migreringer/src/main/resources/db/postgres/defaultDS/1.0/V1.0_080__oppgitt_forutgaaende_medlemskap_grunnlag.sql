create sequence seq_oppgitt_fmedlemskap_holder increment by 50 minvalue 1000000;
create sequence seq_oppgitt_fmedlemskap increment by 50 minvalue 1000000;
create sequence seq_gr_oppgitt_fmedlemskap increment by 50 minvalue 1000000;
create sequence seq_oppgitt_fmedlemskap_bosted increment by 50 minvalue 1000000;

create table oppgitt_fmedlemskap_holder
(
    id            bigint                                 not null primary key,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table oppgitt_fmedlemskap_holder is 'Aggregator som samler alle søknadsperioder for forutgående medlemskap. Kan deles mellom behandlinger.';

create table oppgitt_fmedlemskap
(
    id                            bigint                                            not null primary key,
    oppgitt_fmedlemskap_holder_id bigint references oppgitt_fmedlemskap_holder (id) not null,
    journalpost_id                varchar(20)                                       not null,
    periode                       daterange                                         not null,
    opprettet_av                  varchar(20)  default 'VL'                         not null,
    opprettet_tid                 timestamp(3) default current_timestamp            not null,
    endret_av                     varchar(20),
    endret_tid                    timestamp(3)
);

comment on table oppgitt_fmedlemskap is 'Per-søknad oppgitt forutgående medlemskapsperiode med bosteder. Hver rad representerer data fra én søknad.';

create index idx_oppgitt_fmedlemskap_holder on oppgitt_fmedlemskap (oppgitt_fmedlemskap_holder_id);

create table gr_oppgitt_fmedlemskap
(
    id                            bigint                                            not null primary key,
    behandling_id                 bigint references behandling (id)                 not null,
    oppgitt_fmedlemskap_holder_id bigint references oppgitt_fmedlemskap_holder (id) not null,
    aktiv                         boolean      default true                         not null,
    versjon                       bigint       default 0                            not null,
    opprettet_av                  varchar(20)  default 'VL'                         not null,
    opprettet_tid                 timestamp(3) default current_timestamp            not null,
    endret_av                     varchar(20),
    endret_tid                    timestamp(3)
);

comment on table gr_oppgitt_fmedlemskap is 'Grunnlag som knytter en behandling til en holder. Kun én aktiv rad per behandling (unik indeks).';

create index idx_gr_oppgitt_fmedlemskap_behandling on gr_oppgitt_fmedlemskap (behandling_id);
create unique index uidx_gr_oppgitt_fmedlemskap_aktiv on gr_oppgitt_fmedlemskap (behandling_id) where (aktiv = true);

create table oppgitt_fmedlemskap_bosted
(
    id                     bigint                                     not null primary key,
    oppgitt_fmedlemskap_id bigint references oppgitt_fmedlemskap (id) not null,
    periode                daterange                                  not null,
    landkode               varchar(3)                                 not null,
    opprettet_av           varchar(20)  default 'VL'                  not null,
    opprettet_tid          timestamp(3) default current_timestamp     not null,
    endret_av              varchar(20),
    endret_tid             timestamp(3)
);

comment on table oppgitt_fmedlemskap_bosted is 'Enkeltbosted i utlandet innenfor en oppgitt forutgående medlemskapsperiode. Landkode er ISO 3166-1 alpha-3.';

create index idx_oppgitt_fmedlemskap_bosted_fmedlemskap on oppgitt_fmedlemskap_bosted (oppgitt_fmedlemskap_id);
