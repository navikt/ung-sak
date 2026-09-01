-- Fase 0 av felles vilkårsavklaring: felles lagringsmodell for vilkårsavklaringer, slik at flere vilkår enn
-- bosted (bistand, aktivitet, andre livsoppholdsytelser) kan få samme «foreslå -> varsle -> ferdigstill»-flyt.
-- Ingen eksisterende tabeller (bosatt_*) endres eller migreres i denne migreringen.
-- Se dokumentasjon/felles-vilkaarsavklaring-fase-0-felles-modell.md for begrunnelse for designvalgene under.

create sequence seq_gr_vilkaar_avklaring increment by 50 minvalue 1000000;
create sequence seq_vilkaar_avklaring_holder increment by 50 minvalue 1000000;
create sequence seq_vilkaar_periode_avklaring increment by 50 minvalue 1000000;
create sequence seq_vilkaar_periode_avklaring_fores increment by 50 minvalue 1000000;

create table vilkaar_avklaring_holder
(
    id            bigint primary key,
    opprettet_av  varchar(20)  not null default 'VL',
    opprettet_tid timestamp(3) not null default now(),
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table vilkaar_avklaring_holder is
    'Aggregat som samler ferdigstilte vilkårsavklaringer for ett vilkår. Kan deles mellom behandlinger ved revurdering uten endringer.';

-- Ferdigstilte (vedtatte) avklaringer. Akkumuleres på tvers av behandlinger.
create table vilkaar_periode_avklaring
(
    id                          bigint primary key,
    vilkaar_avklaring_holder_id bigint       not null references vilkaar_avklaring_holder (id),
    referanse                   uuid         not null,
    periode                     daterange    not null,
    ikke_oppfylt_aarsak         varchar(100) not null,
    begrunnelse                 text         not null,
    skal_sende_varsel           boolean      not null default false,
    fritekst_til_varsel         text,
    begrunnelse_ikke_varsel     text,
    avklaringtype               varchar(50)  not null,
    vurdert_av                  varchar(100),
    vurdert_tidspunkt           timestamp(3) not null,
    opprettet_av                varchar(20)  not null default 'VL',
    opprettet_tid               timestamp(3) not null default now(),
    endret_av                   varchar(20),
    endret_tid                  timestamp(3)
);

comment on table vilkaar_periode_avklaring is
    'Ferdigstilte (vedtatte) vilkårsavklaringer per vilkårsperiode, akkumulert på tvers av behandlinger.';

create index idx_vilkaar_periode_avklaring_holder on vilkaar_periode_avklaring (vilkaar_avklaring_holder_id);
create index idx_vilkaar_periode_avklaring_ref on vilkaar_periode_avklaring (referanse);

create table gr_vilkaar_avklaring
(
    id                  bigint primary key,
    behandling_id       bigint      not null,
    vilkaar_type        varchar(50) not null,
    avklaring_holder_id bigint references vilkaar_avklaring_holder (id),
    aktiv               boolean     not null default true,
    versjon             bigint      not null default 0,
    opprettet_av        varchar(20) not null default 'VL',
    opprettet_tid       timestamp(3) not null default now(),
    endret_av           varchar(20),
    endret_tid          timestamp(3)
);

comment on table gr_vilkaar_avklaring is
    'Grunnlag som kobler en behandling og ett vilkår til avklarings-aggregatet. Én aktiv rad per behandling og vilkårstype.';

create unique index uidx_gr_vilkaar_avklaring_aktiv
    on gr_vilkaar_avklaring (behandling_id, vilkaar_type) where aktiv;
create index idx_gr_vilkaar_avklaring_behandling on gr_vilkaar_avklaring (behandling_id);

-- Avklaringer foreslått og behandlet i behandlingen som eier grunnlaget. Kopieres aldri til nye behandlinger.
create table vilkaar_periode_avklaring_foreslaatt
(
    id                      bigint primary key,
    gr_vilkaar_avklaring_id bigint       not null references gr_vilkaar_avklaring (id),
    referanse               uuid         not null,
    periode                 daterange    not null,
    ikke_oppfylt_aarsak     varchar(100) not null,
    begrunnelse             text         not null,
    skal_sende_varsel       boolean      not null default false,
    fritekst_til_varsel     text,
    begrunnelse_ikke_varsel text,
    avklaringtype           varchar(50)  not null,
    vurdert_av              varchar(100),
    vurdert_tidspunkt       timestamp(3) not null,
    opprettet_av            varchar(20)  not null default 'VL',
    opprettet_tid           timestamp(3) not null default now(),
    endret_av               varchar(20),
    endret_tid              timestamp(3)
);

comment on table vilkaar_periode_avklaring_foreslaatt is
    'Vilkårsavklaringer foreslått og behandlet i behandlingen som eier grunnlaget. Henger på grunnlaget, ikke på holderen, fordi de aldri deles mellom behandlinger.';
comment on column vilkaar_periode_avklaring_foreslaatt.ikke_oppfylt_aarsak is
    'Kode fra vilkårets egen *IkkeOppfyltÅrsak-enum. Lagres som tekst fordi hvert vilkår har sitt eget kodeverk.';
comment on column vilkaar_periode_avklaring_foreslaatt.referanse is
    'Stabil referanse brukt som grunnlag_ref i etterlysning og uttalelse. Endres kun når innholdet endres.';

create index idx_vilkaar_periode_avkl_fores_grunnlag on vilkaar_periode_avklaring_foreslaatt (gr_vilkaar_avklaring_id);
create index idx_vilkaar_periode_avkl_fores_ref on vilkaar_periode_avklaring_foreslaatt (referanse);
