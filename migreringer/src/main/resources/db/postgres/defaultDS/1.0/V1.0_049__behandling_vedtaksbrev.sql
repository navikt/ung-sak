alter table brevbestilling
    alter column mottaker_id drop not null;

alter table brevbestilling
    alter column mottaker_id_type drop not null;

create sequence if not exists seq_behandling_vedtaksbrev start with 1 increment by 50;

create table behandling_vedtaksbrev
(
    id                bigint primary key,
    behandling_id     bigint                                                        not null,
    fagsak_id         bigint                                                        not null,
    resultat_type     varchar(20)                                                   not null,
    forklaring        text,
    brevbestilling_id bigint,
    opprettet_av      varchar(20)                    default 'VL'                   not null,
    opprettet_tid     timestamp(3) without time zone default timezone('utc', now()) not null,
    endret_av         varchar(20),
    endret_tid        timestamp(3) without time zone,
    constraint fk_brevbestilling foreign key (brevbestilling_id) references brevbestilling (id),
    constraint fk_behandling_id foreign key (behandling_id) references behandling (id),
    constraint fk_fagsak_id foreign key (fagsak_id) references fagsak (id)
);


comment on column behandling_vedtaksbrev.resultat_type is 'resultat av vurdering for vedtaksbrev';
comment on column behandling_vedtaksbrev.brevbestilling_id is 'brevbestilling id hvis det ble bestilt et vedtaksbrev';

