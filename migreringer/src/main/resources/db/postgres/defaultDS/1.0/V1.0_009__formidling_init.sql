-- Brevbestilling table and sequence
create sequence if not exists seq_brevbestilling start with 1 increment by 50;

create table brevbestilling
(
    id                    bigint primary key,
    brevbestilling_uuid   uuid                                                                not null unique,
    saksnummer            character varying(19)                                               not null,
    dokumentmal_type      character varying(100)                                              not null,
    template_type         character varying(100),
    status                character varying(30)                                               not null,
    dokumentdata          jsonb,
    mottaker_id           character varying(50)                                               not null,
    mottaker_id_type      character varying(10)                                               not null,
    journalpost_id        character varying(20),
    dokdist_bestilling_id character varying(36),
    opprettet_av          character varying(20)          default 'VL'::character varying      not null,
    opprettet_tid         timestamp(3) without time zone default timezone('utc'::text, now()) not null,
    endret_av             character varying(20),
    endret_tid            timestamp(3) without time zone,
    aktiv                 boolean                        default true                         not null,
    versjon               bigint                         default 0                            not null
);

comment on column brevbestilling.brevbestilling_uuid is 'uuid for bestilling, typisk brukt mot dokarkiv';
comment on column brevbestilling.saksnummer is 'saksnummer dokumentet er journalført på, kan også være GENERELL_SAK';
comment on column brevbestilling.dokumentmal_type is 'kode for dokumentmalen som ønskes bestilt';
comment on column brevbestilling.dokumentmal_type is 'kode for template typen som ble brukt til å lage dokument';
comment on column brevbestilling.status is 'status på bestilling.';
comment on column brevbestilling.dokumentdata is 'data for maler der som er manuelt skrevet feks fritekster';
comment on column brevbestilling.mottaker_id is 'Id på ønsket mottaker for dokumentet';
comment on column brevbestilling.mottaker_id_type is 'Type id brukt for identifisering av mottaker, aktørid eller orgnr';
comment on column brevbestilling.journalpost_id is 'Journalpost id fra dokarkiv for journalførte bestillinger';
comment on column brevbestilling.dokdist_bestilling_id is 'Bestillingsid fra dokumentdistribusjonssystemet for distribuerte bestillinger ';


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
    aktiv             boolean                        default true                         not null,
    constraint fk_brevbestilling foreign key (brevbestilling_id) references brevbestilling (id)
);

-- Kun ett vedtaksbrev per behandling
create unique index uix_brevbestilling_behandling_vedtaksbrev
    on brevbestilling_behandling (behandling_id)
    where vedtaksbrev = true and aktiv = true;

create index idx_brevbestilling_behandling_id
    on brevbestilling_behandling (behandling_id);

comment on column brevbestilling_behandling.vedtaksbrev is 'er dokumentet et vedtaksbrev - kun tillatt med ett slik per behandling';
