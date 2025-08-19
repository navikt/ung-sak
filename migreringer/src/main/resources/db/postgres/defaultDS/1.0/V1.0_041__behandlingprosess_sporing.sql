create table if not exists behandlingprosess_sporing
(
    id                      bigint not null primary key,
    behandling_id           bigint not null references behandling(id),
    prosess_input           text not null,
    prosess_resultat        text not null,
    prosess_identifikator   varchar(100) not null,
    opprettet_tid           timestamp default CURRENT_TIMESTAMP not null,
    opprettet_av            varchar(20) not null default 'VL',
    endret_av               varchar(20),
    endret_tid              timestamp
);

create sequence if not exists seq_behandlingprosess_sporing increment by 50 minvalue 1000000;

create index idx_behandlingprosess_sporing_behandling_id on behandlingprosess_sporing (behandling_id);

comment on table behandlingprosess_sporing is 'Inneholder input og resultat for en prosess på behandlingen.';
comment on column behandlingprosess_sporing.prosess_input is 'Input til prosessen, typisk JSON-serialisert objekt.';
comment on column behandlingprosess_sporing.prosess_resultat is 'Resultat fra prosessen, typisk JSON-serialisert objekt.';
