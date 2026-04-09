create sequence seq_gr_oppgitt_fmedlemskap increment by 50 minvalue 1000000;
create sequence seq_oppgitt_fmedlemskap_bosted increment by 50 minvalue 1000000;

create table gr_oppgitt_fmedlemskap
(
    id                      bigint                                 not null primary key,
    behandling_id           bigint references behandling (id)      not null,
    periode                 daterange                              not null,
    aktiv                   boolean      default true              not null,
    versjon                 bigint       default 0                 not null,
    opprettet_av            varchar(20)  default 'VL'              not null,
    opprettet_tid           timestamp(3) default current_timestamp not null,
    endret_av               varchar(20),
    endret_tid              timestamp(3)
);

create index idx_gr_oppgitt_fmedlemskap_behandling on gr_oppgitt_fmedlemskap (behandling_id);
create unique index uidx_gr_oppgitt_fmedlemskap_aktiv on gr_oppgitt_fmedlemskap (behandling_id) where (aktiv = true);

create table oppgitt_fmedlemskap_bosted
(
    id                          bigint                                 not null primary key,
    gr_oppgitt_fmedlemskap_id   bigint references gr_oppgitt_fmedlemskap (id) not null,
    periode                     daterange                              not null,
    landkode                    varchar(3)                              not null,
    opprettet_av                varchar(20)  default 'VL'              not null,
    opprettet_tid               timestamp(3) default current_timestamp not null,
    endret_av                   varchar(20),
    endret_tid                  timestamp(3)
);

create index idx_oppgitt_fmedlemskap_bosted_gr on oppgitt_fmedlemskap_bosted (gr_oppgitt_fmedlemskap_id);
