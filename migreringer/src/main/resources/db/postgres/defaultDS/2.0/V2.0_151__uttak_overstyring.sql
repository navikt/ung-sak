create table if not exists overstyrt_uttak_periode
(
    id                bigint                                 not null primary key,
    behandling_id     bigint                                 not null,
    fom               date                                   not null,
    tom               date                                   not null,
    soeker_uttaksgrad numeric(19, 4)                         null,
    begrunnelse       varchar(4000)                          not null,
    aktiv             boolean      default false             not null,
    versjon           bigint       default 0                 not null,
    opprettet_av      varchar(20)  default 'vl'              not null,
    opprettet_tid     timestamp(3) default current_timestamp not null,
    endret_av         varchar(20),
    endret_tid        timestamp(3)
);


create table if not exists overstyrt_uttak_utbetalingsgrad
(
    id                         bigint                                 not null primary key,
    overstyrt_uttak_periode_id bigint                                 not null,
    aktivitet_type             varchar(100)                           not null,
    arbeidsgiver_orgnr         varchar(100)                           null,
    arbeidsgiver_aktoer_id     varchar(100)                           null,
    utbetalingsgrad            numeric(19, 4)                         not null,
    versjon                    bigint       default 0                 not null,
    opprettet_av               varchar(20)  default 'vl'              not null,
    opprettet_tid              timestamp(3) default current_timestamp not null,
    endret_av                  varchar(20),
    endret_tid                 timestamp(3)
);

alter table overstyrt_uttak_periode
    add constraint fk_overstyrt_uttak_periode_01 foreign key (behandling_id) references behandling (id);
alter table overstyrt_uttak_utbetalingsgrad
    add constraint fk_overstyrt_uttak_utbetalingsgrad_01 foreign key (overstyrt_uttak_periode_id) references overstyrt_uttak_periode (id);

create index idx_overstyrt_uttak_periode_1 on overstyrt_uttak_periode (behandling_id);
create index idx_overstyrt_uttak_utbetalingsgrad_1 on overstyrt_uttak_utbetalingsgrad (overstyrt_uttak_periode_id);

create sequence if not exists seq_overstyrt_uttak_periode increment by 50 minvalue 1000000;
create sequence if not exists seq_overstyrt_uttak_utbetalingsgrad increment by 50 minvalue 1000000;
