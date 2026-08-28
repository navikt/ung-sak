-- Skiller foreslåtte bostedsavklaringer fra ferdigstilte ved å legge dem i hver sin tabell.
-- Tilhørighet til tabell erstatter kolonnen status på bosatt_periode_avklaring:
--   bosatt_periode_avklaring_foreslaatt = foreslått og behandlet i behandlingen som eier holderen
--   bosatt_periode_avklaring            = ferdigstilte (vedtatte) avklaringer, akkumulert på tvers av behandlinger
-- Foreslåtte avklaringer kopieres aldri videre til en ny behandling.
-- De to variantene er frittstående entiteter uten arv, og har derfor hver sin sekvens.

-- Indeksen fra V1.0_093 er ikke lenger relevant
drop index if exists uidx_bosatt_periode_avklaring_referanse_avklares;

create sequence seq_bosatt_periode_avklaring_foreslaatt increment by 50 minvalue 1000000;

create table bosatt_periode_avklaring_foreslaatt
(
    id                         bigint                                         not null primary key,
    bosatt_avklaring_holder_id bigint references bosatt_avklaring_holder (id) not null,
    referanse                  uuid                                           not null,
    periode                    daterange                                      not null,
    ikke_oppfylt_aarsak        varchar(100),
    begrunnelse                text,
    skal_sende_varsel          boolean,
    fritekst_til_varsel        text,
    begrunnelse_ikke_varsel    text,
    avklaringtype              varchar(50)                                    not null,
    vurdert_av                 varchar(100),
    vurdert_tidspunkt          timestamp,
    opprettet_av               varchar(20)  default 'VL'                      not null,
    opprettet_tid              timestamp(3) default current_timestamp         not null,
    endret_av                  varchar(20),
    endret_tid                 timestamp(3)
);

comment on table bosatt_periode_avklaring_foreslaatt is 'Bostedsavklaringer som er foreslått og behandlet i behandlingen som eier holderen. Kopieres ikke til nye behandlinger.';

create index idx_bosatt_periode_avkl_foreslaatt_holder on bosatt_periode_avklaring_foreslaatt (bosatt_avklaring_holder_id);
create index idx_bosatt_periode_avkl_foreslaatt_referanse on bosatt_periode_avklaring_foreslaatt (referanse);

-- Flytter avklaringer under arbeid over i den nye tabellen. Kun ment for å ikke ødelegge saker i test.
insert into bosatt_periode_avklaring_foreslaatt (id, bosatt_avklaring_holder_id, referanse, periode, ikke_oppfylt_aarsak,
                                                 begrunnelse, skal_sende_varsel, fritekst_til_varsel,
                                                 begrunnelse_ikke_varsel, avklaringtype, vurdert_av, vurdert_tidspunkt,
                                                 opprettet_av, opprettet_tid, endret_av, endret_tid)
select bpa.id,
       bpa.bosatt_avklaring_holder_id,
       bpa.referanse,
       bpa.periode,
       bpa.ikke_oppfylt_aarsak,
       bpa.begrunnelse,
       bpa.skal_sende_varsel,
       bpa.fritekst_til_varsel,
       bpa.begrunnelse_ikke_varsel,
       bpa.avklaringtype,
       bpa.vurdert_av,
       bpa.vurdert_tidspunkt,
       bpa.opprettet_av,
       bpa.opprettet_tid,
       bpa.endret_av,
       bpa.endret_tid
from bosatt_periode_avklaring bpa
where bpa.status = 'UNDER_ARBEID';

-- Avklaringer under arbeid er ikke ferdigstilte og hører derfor ikke hjemme i bosatt_periode_avklaring lenger.
delete from bosatt_periode_avklaring where status = 'UNDER_ARBEID';

alter table bosatt_periode_avklaring
    drop column if exists status;


-- Holderen inneholder både foreslåtte og ferdigstilte avklaringer, og heter derfor ikke lenger "foreslatt".
alter table gr_bosatt_avklaring
    rename column foreslatt_holder_id to avklaring_holder_id;
