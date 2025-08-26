create table part
(
    id                  bigint                                       not null   constraint pk_part primary key,
    identifikasjon      varchar(50)                                  not null,
    identifikasjon_type varchar(15)                                  not null,
    rolle_type          varchar(15)                                  not null,
    opprettet_av        varchar(20)  default 'VL'::character varying not null,
    opprettet_tid       timestamp(3) default CURRENT_TIMESTAMP       not null,
    endret_av           varchar(20),
    endret_tid          timestamp(3)
);
create sequence if not exists SEQ_PART increment by 50 minvalue 1000000;

comment on column part.identifikasjon is 'AktørId, Fødselsnummer eller Organisasjonsnummer på virksomhet';
comment on column part.identifikasjon_type is 'Angir typen identifikasjon (AKTØRID, FNR, ORGNR)';
comment on column part.rolle_type is 'Angir rolletypen til parten (ARBEIDSGIVER, BRUKER)';


create table klage_formkrav
(
    id                 bigint                                       not null    constraint pk_klage_formkrav primary key,
    gjelder_vedtak     boolean                                      not null,
    er_klager_part     boolean                                      not null,
    er_frist_overholdt boolean                                      not null,
    er_konkret         boolean                                      not null,
    er_signert         boolean                                      not null,
    begrunnelse        varchar(2000)                                not null,
    opprettet_av       varchar(20)  default 'VL'::character varying not null,
    opprettet_tid      timestamp(3) default CURRENT_TIMESTAMP       not null,
    endret_av          varchar(20),
    endret_tid         timestamp(3)
);
create sequence if not exists SEQ_KLAGE_FORMKRAV increment by 50 minvalue 1000000;

comment on table  klage_formkrav is 'Vurdering av formkrav fra nk/nfp';
comment on column klage_formkrav.id is 'Ref til klagevurdering';
comment on column klage_formkrav.gjelder_vedtak is 'Gjelder vedtak';
comment on column klage_formkrav.er_klager_part is 'Er klager part';
comment on column klage_formkrav.er_frist_overholdt is 'Er frist overholdt';
comment on column klage_formkrav.er_konkret is 'Er konkret';
comment on column klage_formkrav.er_signert is 'Er signert';
comment on column klage_formkrav.begrunnelse is 'Begrunnelse';


create table klage_utredning(
    id                          bigint                                       not null   constraint klage_resultat_pkey primary key,
    behandling_id               bigint                                       not null   constraint fk_klage_resultat_1 references behandling,
    paaklaget_behandling_id     bigint                                                  constraint fk_klage_resultat_2 references behandling,
    opprettet_av                varchar(20)  default 'VL'::character varying not null,
    opprettet_tid               timestamp(3) default CURRENT_TIMESTAMP       not null,
    endret_av                   varchar(20),
    endret_tid                  timestamp(3),
    behandlende_enhet           varchar(10),
    godkjent_av_medunderskriver boolean      default false                   not null,
    formkrav_id                 bigint                                                  constraint fk_klage_formkrav references klage_formkrav,
    part_id                     bigint                                                  constraint fk_part references part
);
create sequence if not exists SEQ_KLAGE_UTREDNING increment by 50 minvalue 1000000;

comment on table  klage_utredning is 'Tabell som kobler klageresultater til klagebehandling/påklagd behandling';
comment on column klage_utredning.behandling_id is 'Ref til klagebehandling';
comment on column klage_utredning.paaklaget_behandling_id is 'Ref til påklaget behandling';
comment on column klage_utredning.behandlende_enhet is 'Navn på opprinnelig behandlende enhet. Dvs. enheten som gjorde det opprinnelige vedtaket som det her klages på';
comment on column klage_utredning.godkjent_av_medunderskriver is 'Har medunderskriver hos KA godkjent klagen';

create index idx_klage_resultat_1 on klage_utredning (behandling_id);
create index idx_klage_resultat_2 on klage_utredning (paaklaget_behandling_id);


create table klage_vurdering(
    id                      bigint                                       not null   constraint pk_klage_vurdering primary key,
    klage_utredning_id      bigint,
    klage_vurdert_av        varchar(3)                                   not null,
    klagevurdering          varchar(100),
    klage_omgjoer_aarsak    varchar(100),
    klage_vurdering_omgjoer varchar(100),
    begrunnelse             text,
    opprettet_av            varchar(20)  default 'VL'::character varying not null,
    opprettet_tid           timestamp(3) default CURRENT_TIMESTAMP       not null,
    endret_av               varchar(20),
    endret_tid              timestamp(3),
    hjemmel                 varchar(100),
    kabal_referanse         varchar(50)
);
create sequence if not exists SEQ_KLAGE_VURDERING increment by 50 minvalue 1000000;

comment on table  klage_vurdering is 'Inneholder vurdering av klage gjort av NK/NFP';
comment on column klage_vurdering.id is 'Primary Key';
comment on column klage_vurdering.klage_utredning_id is 'Ref til klageutredning';
comment on column klage_vurdering.klage_vurdert_av is 'Angir hvem som har vurdert klage (NK = Nav Klageinstans, NFP = Nav Familie og Pensjon)';
comment on column klage_vurdering.klagevurdering is 'Angir vurdering av klage (avvist, medhold, stadfeste, oppheve)';
comment on column klage_vurdering.klage_omgjoer_aarsak is 'Angir årsak dersom vurdering er medhold (nye opplysninger, ulik regelverkstolkning, ulik vurdering, prosessuell feil)';
comment on column klage_vurdering.klage_vurdering_omgjoer is 'Type omgjøring av klagen';
comment on column klage_vurdering.begrunnelse is 'Begrunnelse for vurdering gjort i klage';
comment on column klage_vurdering.kabal_referanse is 'Referanse til klageinstansens saksbehandlingssystem hvis klagen har vært oversendt dit';


create table klage_fritekst
(
    id                  bigint                                       not null constraint pk_klage_fritekst  primary key,
    behandling_id       bigint                                       not null,
    fritekst_skrevet_av varchar(10)                                  not null,
    fritekst            text,
    opprettet_av        varchar(20)  default 'VL'::character varying not null,
    opprettet_tid       timestamp(3) default CURRENT_TIMESTAMP       not null,
    endret_av           varchar(20),
    endret_tid          timestamp(3)
);
create sequence if not exists SEQ_FRITEKST increment by 50 minvalue 1000000;

comment on column klage_fritekst.fritekst_skrevet_av is 'Enheten som har skrevet friteksten';
comment on column klage_fritekst.fritekst is 'Fritekst felt brukt i brev';
