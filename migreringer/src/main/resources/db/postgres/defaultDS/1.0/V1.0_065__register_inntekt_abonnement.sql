create table register_inntekt_abonnement
(
    id                bigint                                    not null    primary key,
    abonnement_id     varchar(100)                              not null    unique,
    aktoer_id         varchar(50)                               not null,
    opprettet_tid     timestamp     default CURRENT_TIMESTAMP   not null,
    opprettet_av      varchar(20)   default 'VL'                not null,
    endret_av         varchar(20),
    endret_tid        timestamp
);

create index idx_register_inntekt_abonnement_aktoer_id
    on register_inntekt_abonnement (aktoer_id);

create sequence seq_register_inntekt_abonnement increment by 50 minvalue 1000000;

comment on table register_inntekt_abonnement is 'Kobling mellom abonnement-ID og aktør-ID for abonnementer i inntektskomponenten.';
comment on column register_inntekt_abonnement.id is 'Primary Key';
comment on column register_inntekt_abonnement.abonnement_id is 'Unik abonnement identifikator for spørring mot inntekskomponenten';
comment on column register_inntekt_abonnement.aktoer_id is 'Aktør-ID som eier abonnementet';

