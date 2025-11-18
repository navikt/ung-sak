create table inntekt_abonnement
(
    id                bigint                                       not null    primary key,
    abonnement_id     varchar(100)                                 not null,
    aktoer_id         varchar(50)                                  not null,
    periode           daterange                                    not null,
    siste_bruksdag    date                                         not null,
    aktiv             boolean                                      not null,
    versjon           bigint           default 0                   not null,
    opprettet_tid     timestamp(3)     default CURRENT_TIMESTAMP   not null,
    opprettet_av      varchar(20)      default 'VL'                not null,
    endret_av         varchar(20),
    endret_tid        timestamp(3)
);

create unique index uidx_inntekt_abonnement_aktoer_id
    on inntekt_abonnement (aktoer_id) where aktiv = true;

create unique index uidx_inntekt_abonnement_abonnement_id
    on inntekt_abonnement (abonnement_id) where aktiv = true;

create sequence seq_inntekt_abonnement increment by 50 minvalue 1000000;

comment on table inntekt_abonnement is 'Kobling mellom abonnement-ID og aktør-ID for abonnementer i inntektskomponenten.';
comment on column inntekt_abonnement.abonnement_id is 'Unik abonnement identifikator for spørring mot inntekskomponenten';
comment on column inntekt_abonnement.aktoer_id is 'Aktør-ID som eier abonnementet';
comment on column inntekt_abonnement.periode is 'Periode for abonnementet';
comment on column inntekt_abonnement.siste_bruksdag is 'Siste bruksdato for abonnementet';

