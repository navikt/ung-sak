-- Brevbestilling table and sequence
create sequence if not exists seq_brevbestilling start with 1 increment by 50;

create table brevbestilling
(
    id                    bigint primary key,
    brevbestilling_uuid   uuid                                                                not null,
    saksnummer            character varying(19)                                               not null,
    dokumentmal_type      character varying(100)                                              not null,
    status                character varying(30)                                               not null,
    dokumentdata          jsonb,
    mottaker_id           character varying(50)                                               not null,
    mottaker_id_type      character varying(10)                                               not null,
    journalpost_id        character varying(20),
    dokdist_bestilling_id character varying(36),
    opprettet_av          character varying(20)          default 'VL'::character varying      not null,
    opprettet_tid         timestamp(3) without time zone default timezone('utc'::text, now()) not null,
    endret_av             character varying(20),
    endret_tid            timestamp(3) without time zone
);

create index idx_brevbestilling_status
    on brevbestilling (status);

-- Brevbestilling_behandling table and sequence
create sequence if not exists seq_behandling_brevbestilling start with 1 increment by 50;

create table brevbestilling_behandling
(
    id                bigint primary key,
    behandling_id     bigint                                                              not null,
    brevbestilling_id bigint                                                              not null unique,
    vedtaksbrev       boolean                                                             not null,
    opprettet_av      character varying(20)          default 'VL'::character varying      not null,
    opprettet_tid     timestamp(3) without time zone default timezone('utc'::text, now()) not null,
    endret_av         character varying(20),
    endret_tid        timestamp(3) without time zone,
    constraint fk_brevbestilling foreign key (brevbestilling_id) references brevbestilling (id)
);


-- Kun ett vedtaksbrev per behandling
create unique index uix_brevbestilling_behandling_vedtaksbrev
    on brevbestilling_behandling (behandling_id)
    where vedtaksbrev = true;
