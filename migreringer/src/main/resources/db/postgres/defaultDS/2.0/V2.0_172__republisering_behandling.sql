create table publiser_behandling_arbeidstabell
(
    id            bigint                                 not null primary key,
    kjøring_id    uuid                                   not null,
    behandling_id bigint                                 not null,
    status        varchar(20)                            not null,
    endring       text,
    kjøring_type  varchar(20)                            not null,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default CURRENT_TIMESTAMP not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

create sequence if not exists seq_publiser_behandling_arbeidstabell increment by 50 minvalue 1000000;

create index idx_publiser_innsyn_status_kjoring_id on publiser_behandling_arbeidstabell (status, kjøring_id);
create index idx_publiser_innsyn_kjoring_id ON publiser_behandling_arbeidstabell (kjøring_id);

comment on table publiser_behandling_arbeidstabell is 'arbeidstabell for publisering av behandlinger';
comment on column publiser_behandling_arbeidstabell.endring is 'satt hvis status = FEILET';
comment on column publiser_behandling_arbeidstabell.kjøring_id is 'unik for kjøringen';
