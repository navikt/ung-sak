-- notat aktør
create table if not exists notat_aktoer
(
    id            bigint                                 not null primary key,
    uuid          uuid                                   not null,
    aktoer_id     varchar(20)                            not null,
    ytelse_type   varchar(100)                           not null,
    skjult        boolean                                not null,
    aktiv         boolean      default true              not null,
    versjon       bigint       default 0                 not null,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default CURRENT_TIMESTAMP not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3),
    unique (uuid, versjon)
);

create index if not exists uidx_notat_aktoer ON notat_aktoer (uuid) where aktiv = true;
create index if not exists idx_notat_aktor on notat_aktoer (aktoer_id, ytelse_type);
create sequence if not exists seq_notat_aktoer increment by 50 minvalue 1000000;

comment on table notat_aktoer is 'Notat som gjelder en aktør, foreløpig bare pleietrengende';

create table if not exists notat_aktoer_tekst
(
    id            bigint                                 not null primary key,
    notat_id      bigint references notat_aktoer (id)    not null,
    tekst         text                                   not null,
    aktiv         boolean      default true              not null,
    versjon       bigint       default 0                 not null,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default CURRENT_TIMESTAMP not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3),
    unique (notat_id, versjon)
);

create index if not exists uidx_notat_aktoer_tekst ON notat_aktoer_tekst (notat_id) where aktiv = true;
create sequence if not exists seq_notat_aktoer_tekst increment by 50 minvalue 1000000;
comment on column notat_aktoer_tekst.tekst is 'Tekst i notatet';
comment on column notat_aktoer_tekst.versjon is 'Versjon av notat teksten med notat_id';


-- notat sak
create table if not exists notat_sak
(
    id            bigint                                 not null primary key,
    uuid          uuid                                   not null,
    fagsak_id     bigint references fagsak (id)          not null,
    skjult        boolean                                not null,
    aktiv         boolean      default true              not null,
    versjon       bigint       default 0                 not null,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default CURRENT_TIMESTAMP not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3),
    unique (uuid, versjon)
);

create index if not exists uidx_notat_sak ON notat_sak (uuid) where aktiv = true;
create index if not exists idx_notat_sak on notat_sak (fagsak_id);
create sequence if not exists seq_notat_sak increment by 50 minvalue 1000000;

comment on table notat_sak is 'Notat som gjelder en fagsak';

create table if not exists notat_sak_tekst
(
    id            bigint                                 not null primary key,
    notat_id      bigint references notat_sak (id)       not null,
    tekst         text                                   not null,
    aktiv         boolean      default true              not null,
    versjon       bigint       default 0                 not null,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default CURRENT_TIMESTAMP not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3),
    unique (notat_id, versjon)
);

create index if not exists uidx_notat_sak_tekst ON notat_sak_tekst (notat_id) where aktiv = true;
create sequence if not exists seq_notat_sak_tekst increment by 50 minvalue 1000000;
comment on column notat_sak_tekst.tekst is 'Tekst i notatet';
comment on column notat_sak_tekst.versjon is 'Versjon av notat teksten med notat_id';
