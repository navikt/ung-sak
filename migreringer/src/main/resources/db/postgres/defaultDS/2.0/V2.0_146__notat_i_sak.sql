create table if not exists notat
(
    id                    bigint                                 not null primary key,
    notat_tekst           text                                   not null,
    gjelder_aktoer_id     varchar(20)                            not null,
    fagsak_id             bigint references fagsak (id)          not null,
    skjult                boolean      default false             not null,
    aktiv                 boolean      default true             not null,
    erstattet_av_notat_id bigint references notat (id),
    versjon               bigint       default 0                 not null,
    opprettet_av          varchar(20)  default 'VL'              not null,
    opprettet_tid         timestamp(3) default CURRENT_TIMESTAMP not null,
    endret_av             varchar(20),
    endret_tid            timestamp(3),
    constraint erstattet_er_slettet check ( erstattet_av_notat_id is null or aktiv = false)
);

create index if not exists idx_notat_fagsak on notat (fagsak_id);
create index if not exists idx_notat_aktor on notat (gjelder_aktoer_id);

create sequence if not exists SEQ_NOTAT increment by 50 minvalue 1000000;

comment on column notat.notat_tekst is 'Tekst i notatet';
comment on column notat.gjelder_aktoer_id is 'Hvem notatet omhandler, bruker eller pleietrengende';
comment on column notat.fagsak_id is 'Fagsaken notatet gjelder for ';
comment on column notat.skjult is 'Skal notatet skjules';
comment on column notat.aktiv is 'Er notatet aktiv?';
comment on column notat.erstattet_av_notat_id is 'notat som erstattet dette notatet';
