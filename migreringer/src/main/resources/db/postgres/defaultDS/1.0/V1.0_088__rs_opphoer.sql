create sequence seq_rs_opphoer increment by 50 minvalue 1000000;

create table rs_opphoer
(
    id                  bigint                                 not null primary key,
    behandling_id       bigint references behandling (id)     not null,
    skaeringstidspunkt  date                                   not null,
    opphors_dato        date,
    opphors_aarsak      varchar(100),
    kilde               varchar(50)                            not null,
    aktiv               boolean      default true              not null,
    versjon             bigint       default 0                 not null,
    opprettet_av        varchar(20)  default 'VL'              not null,
    opprettet_tid       timestamp(3) default current_timestamp not null,
    endret_av           varchar(20),
    endret_tid          timestamp(3)
);

comment on table rs_opphoer is 'Resultat for opphør av bostedsvilkår per skjæringstidspunkt. Lagrer opphørsdato, opphørsårsak (Avslagsårsak) og kilde.';

create index idx_rs_opphoer_behandling on rs_opphoer (behandling_id);
create unique index uidx_rs_opphoer_aktiv on rs_opphoer (behandling_id, skaeringstidspunkt) where (aktiv = true);
